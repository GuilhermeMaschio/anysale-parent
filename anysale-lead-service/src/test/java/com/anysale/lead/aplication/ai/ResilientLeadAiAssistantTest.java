package com.anysale.lead.aplication.ai;

import com.anysale.lead.domain.model.Lead;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ResilientLeadAiAssistantTest {

    @Test
    void usesRulesWhenExternalProviderIsNotEnabled() {
        OpenAiLeadAiAssistant openAi = mock(OpenAiLeadAiAssistant.class);
        when(openAi.analyzeConversation(any(Lead.class), any())).thenReturn(Optional.empty());

        Lead lead = lead();
        LeadAiDraft draft = new ResilientLeadAiAssistant(new RuleBasedLeadAiAssistant(), openAi)
                .analyzeConversation(lead, List.of());

        assertThat(draft.intent()).isEqualTo("BUYING");
    }

    @Test
    void fallsBackToRulesWhenExternalProviderHasNoUsableResult() {
        OpenAiLeadAiAssistant openAi = mock(OpenAiLeadAiAssistant.class);
        when(openAi.analyzeConversation(any(Lead.class), any())).thenReturn(Optional.empty());

        LeadAiDraft draft = new ResilientLeadAiAssistant(new RuleBasedLeadAiAssistant(), openAi)
                .analyzeConversation(lead(), List.of());

        assertThat(draft.suggestedReply()).contains("Guilherme");
    }

    private Lead lead() {
        Lead lead = new Lead();
        lead.setName("Guilherme");
        lead.setSource("WHATSAPP");
        lead.setLastMessage("Quero comprar uma cadeira");
        return lead;
    }
}
