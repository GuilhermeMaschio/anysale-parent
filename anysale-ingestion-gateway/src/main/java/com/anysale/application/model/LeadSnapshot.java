package com.anysale.application.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LeadSnapshot(
        UUID id,
        String name,
        String email,
        String phone,
        String source,
        String desiredCategory,
        List<String> desiredTags,
        String stage,
        String lastMessage,
        Instant lastInteractionAt,
        String summary,
        String intent,
        Integer score,
        String nextAction
) {
}
