package com.anysale.lead.aplication.ai;

import java.util.List;

public record LeadAiDraft(
        String summary,
        String intent,
        String desiredCategory,
        List<String> desiredTags,
        Integer score,
        String nextAction,
        String suggestedReply
) {
}
