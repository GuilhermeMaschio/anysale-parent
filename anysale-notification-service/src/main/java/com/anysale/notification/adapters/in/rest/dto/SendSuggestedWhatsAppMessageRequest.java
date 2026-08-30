package com.anysale.notification.adapters.in.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SendSuggestedWhatsAppMessageRequest(
        @NotNull UUID leadId,
        String to
) {
}
