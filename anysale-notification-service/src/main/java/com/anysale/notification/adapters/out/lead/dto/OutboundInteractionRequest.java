package com.anysale.notification.adapters.out.lead.dto;

public record OutboundInteractionRequest(
        String message,
        String channel,
        String externalMessageId
) {
}
