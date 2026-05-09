package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record InteractionStatusUpdateRequest(
        @NotBlank String channel,
        @NotBlank String externalMessageId,
        @NotBlank String status,
        Instant statusTimestamp,
        String recipientId,
        String errorCode,
        String errorTitle,
        String errorMessage
) {
}
