package com.anysale.lead.aplication.ai;

import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedLeadAiAssistantTest {

    private final RuleBasedLeadAiAssistant assistant = new RuleBasedLeadAiAssistant();

    @Test
    void infersIntentSummaryTagsAndSuggestedReplyFromConversation() {
        Lead lead = new Lead();
        lead.setName("Guilherme Maschio");
        lead.setSource("WHATSAPP");
        lead.setLastMessage("Quero saber mais sobre cadeira ergonomica e preco");

        Interaction interaction = new Interaction();
        interaction.setDirection("IN");
        interaction.setMessage("Quero saber mais sobre cadeira ergonomica e preco");

        LeadAiDraft draft = assistant.analyzeConversation(lead, List.of(interaction));

        assertThat(draft.intent()).isEqualTo("BUYING");
        assertThat(draft.desiredCategory()).isEqualTo("home-office");
        assertThat(draft.desiredTags()).contains("cadeira", "ergonomica", "preco");
        assertThat(draft.score()).isGreaterThanOrEqualTo(70);
        assertThat(draft.summary()).contains("Guilherme");
        assertThat(draft.suggestedReply()).contains("Guilherme");
    }
}
