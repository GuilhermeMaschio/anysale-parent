package com.anysale.lead.aplication.service;

import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.aplication.ai.LeadAiAssistant;
import com.anysale.lead.aplication.ai.LeadAiDraft;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeadAiService {

    private final LeadJpaRepository leadRepository;
    private final InteractionJpaRepository interactionRepository;
    private final LeadEventPublisher leadEventPublisher;
    private final LeadAiAssistant leadAiAssistant;

    @Transactional
    public Lead enrichLeadFromConversation(UUID leadId) {
        Lead lead = leadRepository.findByIdWithTags(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        List<Interaction> interactions = interactionRepository.findByLead_IdOrderByCreatedAtAsc(leadId);
        LeadAiDraft draft = leadAiAssistant.analyzeConversation(lead, interactions);
        applyDraft(lead, draft);

        Lead savedLead = leadRepository.save(lead);
        leadEventPublisher.publishLeadUpdated(savedLead, "AI_ENRICHMENT_UPDATED");
        return savedLead;
    }

    void applyDraft(Lead lead, LeadAiDraft draft) {
        lead.setSummary(limit(draft.summary(), 2_000));
        lead.setIntent(limit(draft.intent(), 120));
        lead.setScore(draft.score());
        lead.setNextAction(limit(draft.nextAction(), 500));
        lead.setSuggestedReply(limit(draft.suggestedReply(), 2_000));
        lead.setSuggestedReplyGeneratedAt(
                draft.suggestedReply() == null || draft.suggestedReply().isBlank() ? null : Instant.now()
        );

        if (limit(draft.desiredCategory(), 80) != null) {
            lead.setDesiredCategory(limit(draft.desiredCategory(), 80));
        }

        List<String> mergedTags = mergeTags(lead.getDesiredTags(), draft.desiredTags());
        if (!mergedTags.isEmpty()) {
            lead.setDesiredTags(mergedTags);
        }
    }

    private List<String> mergeTags(List<String> existingTags, List<String> aiTags) {
        List<String> mergedTags = new ArrayList<>();

        if (existingTags != null) {
            mergedTags.addAll(existingTags);
        }
        if (aiTags != null) {
            mergedTags.addAll(aiTags);
        }

        return mergedTags.stream()
                .map(value -> limit(value, 64))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String limit(String value, int maximum) {
        String normalized = trimToNull(value);
        return normalized == null || normalized.length() <= maximum ? normalized : normalized.substring(0, maximum);
    }
}
