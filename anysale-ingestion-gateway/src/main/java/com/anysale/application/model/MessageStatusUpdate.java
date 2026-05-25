package com.anysale.application.model;

import java.time.Instant;

public record MessageStatusUpdate(
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
