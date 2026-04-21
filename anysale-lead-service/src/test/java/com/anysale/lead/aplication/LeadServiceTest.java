package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.LeadEnrichmentRequestDto;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadSuggestionJpaRepository;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
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
}
