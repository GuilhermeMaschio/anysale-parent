package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record OutboundInteractionRequest(
        @NotBlank String message,
        @NotBlank String channel,
        String externalMessageId
) {
}
