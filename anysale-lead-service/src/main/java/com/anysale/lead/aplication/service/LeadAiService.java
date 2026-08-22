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
        lead.setSummary(trimToNull(draft.summary()));
        lead.setIntent(trimToNull(draft.intent()));
        lead.setScore(draft.score());
        lead.setNextAction(trimToNull(draft.nextAction()));
        lead.setSuggestedReply(trimToNull(draft.suggestedReply()));
        lead.setSuggestedReplyGeneratedAt(
                draft.suggestedReply() == null || draft.suggestedReply().isBlank() ? null : Instant.now()
        );

        if (trimToNull(draft.desiredCategory()) != null) {
            lead.setDesiredCategory(trimToNull(draft.desiredCategory()));
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
                .map(this::trimToNull)
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
}
