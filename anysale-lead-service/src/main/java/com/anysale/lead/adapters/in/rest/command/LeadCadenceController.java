package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.adapters.in.rest.dto.CadenceStepRequest;
import com.anysale.lead.adapters.in.rest.dto.CadenceStepResponse;
import com.anysale.lead.adapters.in.rest.dto.LeadCadenceResponse;
import com.anysale.lead.aplication.LeadCadenceService;
import com.anysale.lead.domain.model.LeadCadence;
import com.anysale.lead.domain.model.SalesPlaybookStep;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/v1")
public class LeadCadenceController {
    private final LeadCadenceService service;

    public LeadCadenceController(LeadCadenceService service) { this.service = service; }

    @PutMapping("/playbooks/{playbookId}/cadence/steps")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public List<CadenceStepResponse> replaceSteps(@PathVariable UUID playbookId,
                                                   @Valid @RequestBody List<@Valid CadenceStepRequest> steps) {
        return service.replaceSteps(playbookId, steps).stream().map(LeadCadenceController::step).toList();
    }

    @GetMapping("/playbooks/{playbookId}/cadence/steps")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public List<CadenceStepResponse> steps(@PathVariable UUID playbookId) {
        return service.steps(playbookId).stream().map(LeadCadenceController::step).toList();
    }

    @PostMapping("/leads/{leadId}/cadence/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public LeadCadenceResponse start(@PathVariable UUID leadId) { return cadence(service.start(leadId)); }

    @PostMapping("/leads/{leadId}/cadence/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public LeadCadenceResponse pause(@PathVariable UUID leadId) { return cadence(service.pause(leadId)); }

    @PostMapping("/leads/{leadId}/cadence/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public LeadCadenceResponse resume(@PathVariable UUID leadId) { return cadence(service.resume(leadId)); }

    @PostMapping("/leads/{leadId}/cadence/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES_MANAGER')")
    public LeadCadenceResponse cancel(@PathVariable UUID leadId) { return cadence(service.cancel(leadId)); }

    @GetMapping("/leads/{leadId}/cadence")
    public LeadCadenceResponse get(@PathVariable UUID leadId) { return cadence(service.get(leadId)); }

    private static CadenceStepResponse step(SalesPlaybookStep source) {
        return new CadenceStepResponse(source.getId(), source.getPosition(), source.getDelayMinutes(), source.getTitle(),
                source.getTaskType(), source.getPriority(), source.getNote());
    }

    private static LeadCadenceResponse cadence(LeadCadence source) {
        return new LeadCadenceResponse(source.getId(), source.getLead().getId(), source.getPlaybook().getId(),
                source.getPlaybook().getName(), source.getStatus(), source.getNextPosition(), source.getNextActionAt(),
                source.getStartedAt(), source.getPausedAt(), source.getCompletedAt());
    }
}
