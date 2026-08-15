package com.anysale.notification.adapters.out.lead.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LeadContactSnapshot(
        UUID id,
        String phone,
        String suggestedReply
) {
}
