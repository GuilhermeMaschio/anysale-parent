package com.anysale.lead.aplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

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
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadCadenceServiceTest {
    @Mock SalesPlaybookJpaRepository playbooks;
    @Mock SalesPlaybookStepJpaRepository steps;
    @Mock LeadCadenceJpaRepository cadences;
    @Mock LeadJpaRepository leads;
    @Mock LeadTaskJpaRepository tasks;
    @Mock SalesPlaybookService playbookService;
    @Mock TenantContext tenants;
    @InjectMocks LeadCadenceService service;

    private final UUID leadId = UUID.randomUUID();
    private final UUID playbookId = UUID.randomUUID();
    private Lead lead;
    private SalesPlaybook playbook;

    @BeforeEach
    void setUp() {
        lenient().when(tenants.tenantId()).thenReturn("tenant-a");
        lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId("tenant-a");
        playbook = new SalesPlaybook();
        setId(playbook, playbookId);
        playbook.setTenantId("tenant-a");
    }

    @Test
    void startsCadenceWithFirstStepDelay() {
        SalesPlaybookStep first = step(1, 15);
        when(leads.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(playbookService.resolve(leadId)).thenReturn(playbook);
        when(steps.findByPlaybook_IdOrderByPositionAsc(playbookId)).thenReturn(List.of(first));
        when(cadences.findScoped(leadId, "tenant-a")).thenReturn(Optional.empty());
        when(cadences.save(any())).thenAnswer(call -> call.getArgument(0));

        LeadCadence cadence = service.start(leadId);

        assertThat(cadence.getStatus()).isEqualTo("ACTIVE");
        assertThat(cadence.getNextPosition()).isEqualTo(1);
        assertThat(cadence.getNextActionAt()).isAfter(cadence.getStartedAt());
    }

    @Test
    void createsTaskAndCompletesCadenceAtLastStep() {
        SalesPlaybookStep onlyStep = step(1, 0);
        LeadCadence cadence = new LeadCadence();
        cadence.setTenantId("tenant-a");
        cadence.setLead(lead);
        cadence.setPlaybook(playbook);
        cadence.setStatus("ACTIVE");
        cadence.setNextPosition(1);

        when(cadences.findDueForUpdate(any())).thenReturn(List.of(cadence));
        when(steps.findByPlaybook_IdOrderByPositionAsc(playbookId)).thenReturn(List.of(onlyStep));

        service.generateDueTasks();

        ArgumentCaptor<LeadTask> created = ArgumentCaptor.forClass(LeadTask.class);
        verify(tasks).save(created.capture());
        assertThat(created.getValue().getLead()).isSameAs(lead);
        assertThat(created.getValue().getTaskType()).isEqualTo("FOLLOW_UP");
        assertThat(cadence.getStatus()).isEqualTo("COMPLETED");
        assertThat(cadence.getNextActionAt()).isNull();
    }

    @Test
    void pausesActiveCadenceAndCreatesResponseTaskForInboundMessage() {
        LeadCadence cadence = new LeadCadence();
        cadence.setStatus("ACTIVE");
        cadence.setLead(lead);
        cadence.setTenantId("tenant-a");
        lead.setName("Ana");
        when(cadences.findScoped(leadId, "tenant-a")).thenReturn(Optional.of(cadence));

        service.pauseForInboundResponse(lead);

        ArgumentCaptor<LeadTask> created = ArgumentCaptor.forClass(LeadTask.class);
        verify(tasks).save(created.capture());
        assertThat(cadence.getStatus()).isEqualTo("PAUSED");
        assertThat(created.getValue().getTaskType()).isEqualTo("WHATSAPP_REPLY");
        assertThat(created.getValue().getPriority()).isEqualTo("HIGH");
    }

    private SalesPlaybookStep step(int position, int delayMinutes) {
        SalesPlaybookStep step = new SalesPlaybookStep();
        step.setPlaybook(playbook);
        step.setPosition(position);
        step.setDelayMinutes(delayMinutes);
        step.setTitle("Retomar contato");
        step.setTaskType("FOLLOW_UP");
        step.setPriority("NORMAL");
        return step;
    }

    private void setId(SalesPlaybook target, UUID id) {
        try {
            Field field = SalesPlaybook.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
