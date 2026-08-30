package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.LeadEnrichmentRequestDto;
import com.anysale.lead.adapters.in.rest.dto.OutboundInteractionRequest;
import com.anysale.lead.adapters.in.rest.dto.InteractionStatusUpdateRequest;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadSuggestionJpaRepository;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadJpaRepository leadRepo;

    @Mock
    private LeadSuggestionJpaRepository suggestionRepo;

    @Mock
    private InteractionJpaRepository interactionRepo;

    @Mock
    private LeadEventPublisher events;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private LeadService service;

    @Test
    void applyEnrichmentUpdatesLeadFieldsAndPublishesEvent() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setName("Lead");
        lead.setDesiredCategory("old-category");
        lead.setDesiredTags(List.of("old-tag"));
        lead.setUpdatedAt(Instant.now().minusSeconds(60));

        LeadEnrichmentRequestDto request = LeadEnrichmentRequestDto.builder()
                .summary("Cliente quer cadeira ergonômica")
                .intent("BUYING")
                .desiredCategory("home-office")
                .desiredTags(List.of("cadeira", "ergonômica", "cadeira"))
                .score(92)
                .nextAction("Enviar catálogo no WhatsApp")
                .build();

        when(leadRepo.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(leadRepo.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lead saved = service.applyEnrichment(leadId, request);

        assertThat(saved.getSummary()).isEqualTo("Cliente quer cadeira ergonômica");
        assertThat(saved.getIntent()).isEqualTo("BUYING");
        assertThat(saved.getDesiredCategory()).isEqualTo("home-office");
        assertThat(saved.getDesiredTags()).containsExactly("cadeira", "ergonômica");
        assertThat(saved.getScore()).isEqualTo(92);
        assertThat(saved.getNextAction()).isEqualTo("Enviar catálogo no WhatsApp");

        verify(leadRepo).save(lead);
        verify(events).publishLeadUpdated(lead, "ENRICHMENT_UPDATED");
    }

    @Test
    void listInteractionsReturnsConversationHistoryForLead() {
        UUID leadId = UUID.randomUUID();
        Interaction first = new Interaction();
        first.setMessage("Olá");
        Interaction second = new Interaction();
        second.setMessage("Quero saber mais");

        when(leadRepo.existsById(leadId)).thenReturn(true);
        when(interactionRepo.findByLead_IdOrderByCreatedAtAsc(leadId)).thenReturn(List.of(first, second));

        List<Interaction> interactions = service.listInteractions(leadId);

        assertThat(interactions).containsExactly(first, second);
    }

    @Test
    void listInteractionsThrowsWhenLeadDoesNotExist() {
        UUID leadId = UUID.randomUUID();
        when(leadRepo.existsById(leadId)).thenReturn(false);

        assertThatThrownBy(() -> service.listInteractions(leadId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Lead not found");

        verify(interactionRepo, never()).findByLead_IdOrderByCreatedAtAsc(any());
    }

    @Test
    void recordOutboundInteractionPersistsInteractionAndUpdatesLead() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setName("Guilherme");

        when(leadRepo.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(interactionRepo.findByChannelAndExternalMessageId("WHATSAPP", "wamid.outbound.001"))
                .thenReturn(Optional.empty());
        when(interactionRepo.save(any(Interaction.class))).thenAnswer(invocation -> {
            Interaction interaction = invocation.getArgument(0);
            interaction.setId(UUID.randomUUID());
            return interaction;
        });
        when(leadRepo.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interaction saved = service.recordOutboundInteraction(leadId, new OutboundInteractionRequest(
                "Oi, posso te ajudar com a cadeira ergonomica.",
                "whatsapp",
                "wamid.outbound.001"
        ));

        assertThat(saved.getLead()).isEqualTo(lead);
        assertThat(saved.getDirection()).isEqualTo("OUT");
        assertThat(saved.getChannel()).isEqualTo("WHATSAPP");
        assertThat(saved.getExternalMessageId()).isEqualTo("wamid.outbound.001");
        assertThat(lead.getLastMessage()).isEqualTo("Oi, posso te ajudar com a cadeira ergonomica.");
        assertThat(lead.getLastInteractionAt()).isNotNull();

        verify(interactionRepo).save(any(Interaction.class));
        verify(leadRepo).save(lead);
        verify(events).publishLeadUpdated(lead, "OUTBOUND_MESSAGE_SENT");
    }

    @Test
    void recordOutboundInteractionReturnsExistingInteractionWhenExternalMessageIdAlreadyExists() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        Interaction existing = new Interaction();
        existing.setLead(lead);
        existing.setExternalMessageId("wamid.outbound.001");

        when(leadRepo.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(interactionRepo.findByChannelAndExternalMessageId("WHATSAPP", "wamid.outbound.001"))
                .thenReturn(Optional.of(existing));

        Interaction result = service.recordOutboundInteraction(leadId, new OutboundInteractionRequest(
                "Mensagem duplicada",
                "WHATSAPP",
                "wamid.outbound.001"
        ));

        assertThat(result).isEqualTo(existing);
        verify(interactionRepo, never()).save(any(Interaction.class));
        verify(leadRepo, never()).save(any(Lead.class));
        verify(events, never()).publishLeadUpdated(any(Lead.class), any());
    }

    @Test
    void updateInteractionStatusPersistsLatestMetaStatus() {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());

        Interaction interaction = new Interaction();
        interaction.setLead(lead);
        interaction.setExternalMessageId("wamid.outbound.001");
        interaction.setChannel("WHATSAPP");
        interaction.setDeliveryStatusAt(Instant.parse("2026-05-09T12:00:00Z"));

        when(interactionRepo.findByChannelAndExternalMessageId("WHATSAPP", "wamid.outbound.001"))
                .thenReturn(Optional.of(interaction));
        when(interactionRepo.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateInteractionStatus(new InteractionStatusUpdateRequest(
                "whatsapp",
                "wamid.outbound.001",
                "read",
                Instant.parse("2026-05-09T12:05:00Z"),
                "5541999999999",
                null,
                null,
                null
        ));

        assertThat(interaction.getDeliveryStatus()).isEqualTo("READ");
        assertThat(interaction.getDeliveryStatusAt()).isEqualTo(Instant.parse("2026-05-09T12:05:00Z"));
        assertThat(interaction.getDeliveryRecipientId()).isEqualTo("5541999999999");
        assertThat(interaction.getDeliveryErrorCode()).isNull();

        verify(interactionRepo).save(interaction);
        verify(events).publishLeadUpdated(lead, "WHATSAPP_STATUS_READ");
    }

    @Test
    void updateInteractionStatusIgnoresOlderMetaStatusEvent() {
        Interaction interaction = new Interaction();
        interaction.setLead(new Lead());
        interaction.setExternalMessageId("wamid.outbound.001");
        interaction.setChannel("WHATSAPP");
        interaction.setDeliveryStatus("READ");
        interaction.setDeliveryStatusAt(Instant.parse("2026-05-09T12:05:00Z"));

        when(interactionRepo.findByChannelAndExternalMessageId("WHATSAPP", "wamid.outbound.001"))
                .thenReturn(Optional.of(interaction));

        service.updateInteractionStatus(new InteractionStatusUpdateRequest(
                "WHATSAPP",
                "wamid.outbound.001",
                "sent",
                Instant.parse("2026-05-09T12:01:00Z"),
                "5541999999999",
                null,
                null,
                null
        ));

        assertThat(interaction.getDeliveryStatus()).isEqualTo("READ");
        verify(interactionRepo, never()).save(any(Interaction.class));
        verify(events, never()).publishLeadUpdated(any(Lead.class), any());
    }

    @Test
    void recordTestInteractionCopiesTheTenantFromTheLead() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setTenantId("anysale");

        when(leadRepo.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(interactionRepo.save(any(Interaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leadRepo.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Interaction saved = service.recordTestInteraction(leadId, "Mensagem de teste", "IN");

        assertThat(saved.getTenantId()).isEqualTo("anysale");
        assertThat(saved.getMessage()).isEqualTo("Mensagem de teste");
        assertThat(saved.getDirection()).isEqualTo("INBOUND");
    }
}
