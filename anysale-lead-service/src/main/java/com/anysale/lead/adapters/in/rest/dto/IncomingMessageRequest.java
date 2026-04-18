package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record IncomingMessageRequest(
        @NotBlank String phone,
        String leadName,
        @NotBlank String message,
        @NotBlank String channel,
        String externalMessageId
) {
}
