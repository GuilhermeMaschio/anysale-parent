package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.CadenceStepRequest;
import com.anysale.lead.adapters.out.persistence.LeadCadenceJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadTaskJpaRepository;
import com.anysale.lead.adapters.out.persistence.SalesPlaybookJpaRepository;
import com.anysale.lead.adapters.out.persistence.SalesPlaybookStepJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadCadence;
import com.anysale.lead.domain.model.LeadTask;
import com.anysale.lead.domain.model.SalesPlaybook;
import com.anysale.lead.domain.model.SalesPlaybookStep;
import com.anysale.lead.tenant.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeadCadenceService {
    private final SalesPlaybookJpaRepository playbooks;
    private final SalesPlaybookStepJpaRepository steps;
    private final LeadCadenceJpaRepository cadences;
    private final LeadJpaRepository leads;
    private final LeadTaskJpaRepository tasks;
    private final SalesPlaybookService playbookService;
    private final TenantContext tenants;

    public LeadCadenceService(SalesPlaybookJpaRepository playbooks, SalesPlaybookStepJpaRepository steps,
                              LeadCadenceJpaRepository cadences, LeadJpaRepository leads, LeadTaskJpaRepository tasks,
                              SalesPlaybookService playbookService, TenantContext tenants) {
        this.playbooks = playbooks;
        this.steps = steps;
        this.cadences = cadences;
        this.leads = leads;
        this.tasks = tasks;
        this.playbookService = playbookService;
        this.tenants = tenants;
    }

    @Transactional
    public List<SalesPlaybookStep> replaceSteps(UUID playbookId, List<CadenceStepRequest> requests) {
        SalesPlaybook playbook = playbook(playbookId);
        cadences.flush();
        steps.deleteByPlaybook_Id(playbookId);
        List<SalesPlaybookStep> replacements = java.util.stream.IntStream.range(0, requests.size())
                .mapToObj(index -> step(playbook, index + 1, requests.get(index)))
                .toList();
        return steps.saveAll(replacements);
    }

    @Transactional(readOnly = true)
    public List<SalesPlaybookStep> steps(UUID playbookId) {
        playbook(playbookId);
        return steps.findByPlaybook_IdOrderByPositionAsc(playbookId);
    }

    @Transactional
    public LeadCadence start(UUID leadId) {
        Lead lead = lead(leadId);
        if ("WON".equals(lead.getStage()) || "LOST".equals(lead.getStage())) {
            throw badRequest("A cadence cannot be started for a closed lead");
        }
        SalesPlaybook playbook = playbookService.resolve(leadId);
        List<SalesPlaybookStep> playbookSteps = steps.findByPlaybook_IdOrderByPositionAsc(playbook.getId());
        if (playbookSteps.isEmpty()) {
            throw badRequest("The selected playbook does not have cadence steps");
        }
        Instant now = Instant.now();
        LeadCadence cadence = cadences.findScoped(leadId, tenants.tenantId()).orElseGet(LeadCadence::new);
        cadence.setTenantId(lead.getTenantId());
        cadence.setLead(lead);
        cadence.setPlaybook(playbook);
        cadence.setStatus("ACTIVE");
        cadence.setNextPosition(1);
        cadence.setNextActionAt(now.plus(playbookSteps.get(0).getDelayMinutes(), ChronoUnit.MINUTES));
        cadence.setStartedAt(now);
        cadence.setPausedAt(null);
        cadence.setCompletedAt(null);
        return cadences.save(cadence);
    }

    @Transactional
    public LeadCadence pause(UUID leadId) {
        LeadCadence cadence = cadence(leadId);
        if (!"ACTIVE".equals(cadence.getStatus())) throw conflict("Only an active cadence can be paused");
        cadence.setStatus("PAUSED");
        cadence.setPausedAt(Instant.now());
        return cadences.save(cadence);
    }

    /** Pauses only an active cadence; repeated inbound messages do not duplicate the review task. */
    @Transactional
    public void pauseForInboundResponse(Lead lead) {
        cadences.findScoped(lead.getId(), lead.getTenantId())
                .filter(cadence -> "ACTIVE".equals(cadence.getStatus()))
                .ifPresent(cadence -> {
                    cadence.setStatus("PAUSED");
                    cadence.setPausedAt(Instant.now());
                    cadences.save(cadence);

                    LeadTask task = new LeadTask();
                    task.setLead(lead);
                    task.setTenantId(lead.getTenantId());
                    task.setTitle("Responder mensagem de " + lead.getName());
                    task.setTaskType("WHATSAPP_REPLY");
                    task.setPriority("HIGH");
                    task.setDueAt(Instant.now());
                    task.setNote("Cadência pausada após resposta do cliente.");
                    tasks.save(task);
                });
    }

    @Transactional
    public LeadCadence resume(UUID leadId) {
        LeadCadence cadence = cadence(leadId);
        if (!"PAUSED".equals(cadence.getStatus())) throw conflict("Only a paused cadence can be resumed");
        SalesPlaybookStep next = nextStep(cadence);
        cadence.setStatus("ACTIVE");
        cadence.setPausedAt(null);
        cadence.setNextActionAt(Instant.now().plus(next.getDelayMinutes(), ChronoUnit.MINUTES));
        return cadences.save(cadence);
    }

    @Transactional
    public LeadCadence cancel(UUID leadId) {
        LeadCadence cadence = cadence(leadId);
        if ("COMPLETED".equals(cadence.getStatus()) || "CANCELLED".equals(cadence.getStatus())) {
            throw conflict("Cadence is already finished");
        }
        cadence.setStatus("CANCELLED");
        cadence.setNextActionAt(null);
        return cadences.save(cadence);
    }

    @Transactional(readOnly = true)
    public LeadCadence get(UUID leadId) {
        return cadence(leadId);
    }

    @Scheduled(fixedDelayString = "${anysale.cadence.poll-interval:PT1M}")
    @Transactional
    public void generateDueTasks() {
        Instant now = Instant.now();
        for (LeadCadence cadence : cadences.findDueForUpdate(now)) {
            SalesPlaybookStep step = nextStep(cadence);
            createTask(cadence, step, now);
            scheduleFollowingStep(cadence, now);
            cadences.save(cadence);
        }
    }

    private void scheduleFollowingStep(LeadCadence cadence, Instant now) {
        List<SalesPlaybookStep> playbookSteps = steps.findByPlaybook_IdOrderByPositionAsc(cadence.getPlaybook().getId());
        int nextPosition = cadence.getNextPosition() + 1;
        if (nextPosition > playbookSteps.size()) {
            cadence.setStatus("COMPLETED");
            cadence.setCompletedAt(now);
            cadence.setNextActionAt(null);
            return;
        }
        SalesPlaybookStep next = playbookSteps.get(nextPosition - 1);
        cadence.setNextPosition(nextPosition);
        cadence.setNextActionAt(now.plus(next.getDelayMinutes(), ChronoUnit.MINUTES));
    }

    private void createTask(LeadCadence cadence, SalesPlaybookStep step, Instant now) {
        LeadTask task = new LeadTask();
        task.setLead(cadence.getLead());
        task.setTenantId(cadence.getTenantId());
        task.setTitle(step.getTitle());
        task.setTaskType(step.getTaskType());
        task.setPriority(step.getPriority());
        task.setDueAt(now);
        task.setNote(step.getNote());
        tasks.save(task);
    }

    private SalesPlaybookStep nextStep(LeadCadence cadence) {
        return steps.findByPlaybook_IdOrderByPositionAsc(cadence.getPlaybook().getId()).stream()
                .filter(step -> step.getPosition() == cadence.getNextPosition())
                .findFirst()
                .orElseThrow(() -> conflict("Cadence step is no longer configured"));
    }

    private SalesPlaybook playbook(UUID id) {
        return playbooks.findById(id)
                .filter(playbook -> tenants.tenantId().equals(playbook.getTenantId()))
                .orElseThrow(() -> notFound("Playbook not found"));
    }

    private Lead lead(UUID id) {
        return leads.findByIdWithTags(id)
                .filter(lead -> tenants.tenantId().equals(lead.getTenantId()))
                .orElseThrow(() -> notFound("Lead not found"));
    }

    private LeadCadence cadence(UUID leadId) {
        return cadences.findScoped(leadId, tenants.tenantId())
                .orElseThrow(() -> notFound("Lead cadence not found"));
    }

    private SalesPlaybookStep step(SalesPlaybook playbook, int position, CadenceStepRequest request) {
        SalesPlaybookStep step = new SalesPlaybookStep();
        step.setPlaybook(playbook);
        step.setPosition(position);
        step.setDelayMinutes(request.delayMinutes());
        step.setTitle(request.title().trim());
        step.setTaskType(request.taskType());
        step.setPriority(request.priority() == null ? "NORMAL" : request.priority());
        step.setNote(trim(request.note()));
        return step;
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
