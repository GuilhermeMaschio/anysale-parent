package com.anysale.notification.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SendWhatsAppMessageRequest(
        UUID leadId,
        @NotBlank String to,
        @NotBlank String message
) {
}
