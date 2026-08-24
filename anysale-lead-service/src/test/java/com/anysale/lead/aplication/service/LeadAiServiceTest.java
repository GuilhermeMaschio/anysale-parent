package com.anysale.lead.aplication.service;

import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.aplication.ai.LeadAiAssistant;
import com.anysale.lead.aplication.ai.LeadAiDraft;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadAiServiceTest {

    @Mock
    private LeadJpaRepository leadRepository;

    @Mock
    private InteractionJpaRepository interactionRepository;

    @Mock
    private LeadEventPublisher leadEventPublisher;

    @Mock
    private LeadAiAssistant leadAiAssistant;

    @InjectMocks
    private LeadAiService service;

    @Test
    void enrichLeadFromConversationPersistsAiFieldsAndPublishesEvent() {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setName("Guilherme");
        lead.setSource("WHATSAPP");
        lead.setLastMessage("Quero saber mais sobre cadeira ergonomica");
        lead.setDesiredTags(List.of("whatsapp"));

        Interaction interaction = new Interaction();
        interaction.setLead(lead);
        interaction.setDirection("IN");
        interaction.setMessage("Quero saber mais sobre cadeira ergonomica");
        interaction.setCreatedAt(Instant.parse("2026-05-24T21:00:00Z"));

        when(leadRepository.findByIdWithTags(leadId)).thenReturn(Optional.of(lead));
        when(interactionRepository.findByLead_IdOrderByCreatedAtAsc(leadId)).thenReturn(List.of(interaction));
        when(leadAiAssistant.analyzeConversation(lead, List.of(interaction)))
                .thenReturn(new LeadAiDraft(
                        "Resumo gerado",
                        "BUYING",
                        "home-office",
                        List.of("cadeira", "ergonomica"),
                        91,
                        "Responder rapido no WhatsApp",
                        "Oi, Guilherme! Posso te mandar algumas opcoes."
                ));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lead savedLead = service.enrichLeadFromConversation(leadId);

        assertThat(savedLead.getSummary()).isEqualTo("Resumo gerado");
        assertThat(savedLead.getIntent()).isEqualTo("BUYING");
        assertThat(savedLead.getDesiredCategory()).isEqualTo("home-office");
        assertThat(savedLead.getDesiredTags()).containsExactly("whatsapp", "cadeira", "ergonomica");
        assertThat(savedLead.getScore()).isEqualTo(91);
        assertThat(savedLead.getNextAction()).isEqualTo("Responder rapido no WhatsApp");
        assertThat(savedLead.getSuggestedReply()).isEqualTo("Oi, Guilherme! Posso te mandar algumas opcoes.");
        assertThat(savedLead.getSuggestedReplyGeneratedAt()).isNotNull();

        verify(leadRepository).save(lead);
        verify(leadEventPublisher).publishLeadUpdated(lead, "AI_ENRICHMENT_UPDATED");
    }

    @Test
    void applyDraftConstrainsAiTextToPersistedColumnLimits() {
        Lead lead = new Lead();
        String oversized = "x".repeat(200);

        service.applyDraft(lead, new LeadAiDraft(
                oversized.repeat(20), oversized, oversized, List.of(oversized), 80,
                oversized.repeat(4), oversized.repeat(20)
        ));

        assertThat(lead.getSummary()).hasSize(2_000);
        assertThat(lead.getIntent()).hasSize(120);
        assertThat(lead.getDesiredCategory()).hasSize(80);
        assertThat(lead.getDesiredTags()).allSatisfy(tag -> assertThat(tag).hasSize(64));
        assertThat(lead.getNextAction()).hasSize(500);
        assertThat(lead.getSuggestedReply()).hasSize(2_000);
    }
}
