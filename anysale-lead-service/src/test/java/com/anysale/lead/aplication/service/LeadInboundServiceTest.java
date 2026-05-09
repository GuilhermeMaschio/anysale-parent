package com.anysale.lead.aplication.service;

import com.anysale.lead.adapters.in.rest.dto.IncomingMessageRequest;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadInboundServiceTest {

    @Mock
    private LeadJpaRepository leadRepository;

    @Mock
    private InteractionJpaRepository interactionRepository;

    @Mock
    private LeadEventPublisher leadEventPublisher;

    @InjectMocks
    private LeadInboundService service;

    @Test
    void createsLeadAndInteractionWhenPhoneDoesNotExist() {
        IncomingMessageRequest request = new IncomingMessageRequest(
                "+55 (41) 99999-9999",
                "Guilherme",
                "Quero saber mais",
                "whatsapp",
                "msg-1"
        );

        when(interactionRepository.findByChannelAndExternalMessageId("WHATSAPP", "msg-1"))
                .thenReturn(Optional.empty());
        when(leadRepository.findAllByNormalizedPhone("5541999999999")).thenReturn(List.of());
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> {
            Lead lead = invocation.getArgument(0);
            if (lead.getId() == null) {
                lead.setId(UUID.randomUUID());
            }
            return lead;
        });

        LeadResponseDto response = service.execute(request);

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        Lead savedLead = leadCaptor.getValue();
        assertThat(savedLead.getPhone()).isEqualTo("5541999999999");
        assertThat(savedLead.getName()).isEqualTo("Guilherme");
        assertThat(savedLead.getSource()).isEqualTo("WHATSAPP");
        assertThat(savedLead.getStage()).isEqualTo("CONTACTED");
        assertThat(savedLead.getLastMessage()).isEqualTo("Quero saber mais");
        assertThat(savedLead.getLastInteractionAt()).isNotNull();
        assertThat(response.getId()).isEqualTo(savedLead.getId());
        assertThat(response.getStage()).isEqualTo("CONTACTED");

        ArgumentCaptor<Interaction> interactionCaptor = ArgumentCaptor.forClass(Interaction.class);
        verify(interactionRepository).save(interactionCaptor.capture());
        Interaction interaction = interactionCaptor.getValue();
        assertThat(interaction.getLead()).isSameAs(savedLead);
        assertThat(interaction.getChannel()).isEqualTo("WHATSAPP");
        assertThat(interaction.getDirection()).isEqualTo("IN");
        assertThat(interaction.getExternalMessageId()).isEqualTo("msg-1");

        verify(leadEventPublisher).publishLeadCreated(savedLead);
        verify(leadEventPublisher).publishLeadUpdated(savedLead, "INCOMING_MESSAGE_RECEIVED");
    }

    @Test
    void reusesExistingLeadWithoutOverwritingExistingName() {
        Lead existingLead = new Lead();
        existingLead.setId(UUID.randomUUID());
        existingLead.setName("Lead Existente");
        existingLead.setPhone("+55 (41) 99999-9999");
        existingLead.setStage("NEW");
        existingLead.setCreatedAt(Instant.now().minusSeconds(3600));

        IncomingMessageRequest request = new IncomingMessageRequest(
                "41999999999",
                "Novo Nome",
                "Mensagem recebida",
                "WHATSAPP",
                null
        );

        when(leadRepository.findAllByNormalizedPhone("41999999999")).thenReturn(List.of(existingLead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadResponseDto response = service.execute(request);

        verify(leadRepository).save(existingLead);
        assertThat(existingLead.getName()).isEqualTo("Lead Existente");
        assertThat(existingLead.getPhone()).isEqualTo("41999999999");
        assertThat(existingLead.getStage()).isEqualTo("CONTACTED");
        assertThat(existingLead.getLastMessage()).isEqualTo("Mensagem recebida");
        assertThat(response.getId()).isEqualTo(existingLead.getId());
        assertThat(response.getLastMessage()).isEqualTo("Mensagem recebida");

        verify(leadEventPublisher, never()).publishLeadCreated(any(Lead.class));
        verify(leadEventPublisher).publishLeadUpdated(existingLead, "INCOMING_MESSAGE_RECEIVED");
    }

    @Test
    void ignoresDuplicatedExternalMessage() {
        IncomingMessageRequest request = new IncomingMessageRequest(
                "41999999999",
                "Guilherme",
                "Quero saber mais",
                "WHATSAPP",
                "msg-duplicada"
        );

        Interaction interaction = new Interaction();
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setName("Lead duplicado");
        interaction.setLead(lead);
        when(interactionRepository.findByChannelAndExternalMessageId("WHATSAPP", "msg-duplicada"))
                .thenReturn(Optional.of(interaction));
        when(leadRepository.findByIdWithTags(lead.getId())).thenReturn(Optional.of(lead));

        LeadResponseDto response = service.execute(request);

        verify(leadRepository, never()).save(any(Lead.class));
        verify(interactionRepository, never()).save(any(Interaction.class));
        verify(leadEventPublisher, never()).publishLeadCreated(any(Lead.class));
        verify(leadEventPublisher, never()).publishLeadUpdated(any(Lead.class), eq("INCOMING_MESSAGE_RECEIVED"));
        assertThat(response.getId()).isEqualTo(lead.getId());
    }

    @Test
    void usesFallbackNameWhenInboundLeadNameIsMissing() {
        IncomingMessageRequest request = new IncomingMessageRequest(
                "41999999999",
                "   ",
                "Mensagem recebida",
                "WHATSAPP",
                null
        );

        when(leadRepository.findAllByNormalizedPhone("41999999999")).thenReturn(List.of());
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadResponseDto response = service.execute(request);

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        assertThat(leadCaptor.getValue().getName()).isEqualTo("Contato 41999999999");
        assertThat(response.getName()).isEqualTo("Contato 41999999999");
    }
}
