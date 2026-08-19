package com.anysale.lead.aplication.ai;

import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/** Uses the optional external provider when configured and preserves local rules as a safe fallback. */
@Component
@Primary
@RequiredArgsConstructor
public class ResilientLeadAiAssistant implements LeadAiAssistant {

    private final RuleBasedLeadAiAssistant ruleBasedLeadAiAssistant;
    private final OpenAiLeadAiAssistant openAiLeadAiAssistant;

    @Override
    public LeadAiDraft analyzeConversation(Lead lead, List<Interaction> interactions) {
        return openAiLeadAiAssistant.analyzeConversation(lead, interactions)
                .orElseGet(() -> ruleBasedLeadAiAssistant.analyzeConversation(lead, interactions));
    }
}
