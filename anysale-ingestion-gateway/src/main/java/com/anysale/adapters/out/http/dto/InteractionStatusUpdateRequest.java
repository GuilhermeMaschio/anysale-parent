package com.anysale.adapters.out.http.dto;

import java.time.Instant;

public record InteractionStatusUpdateRequest(
        String channel,
        String externalMessageId,
        String status,
        Instant statusTimestamp,
        String recipientId,
        String errorCode,
        String errorTitle,
        String errorMessage
) {
}
