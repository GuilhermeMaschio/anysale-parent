package com.anysale.notification.adapters.in.rest.dto;

import java.util.UUID;

public record SendWhatsAppMessageResponse(
        UUID leadId,
        String to,
        String waId,
        String messageId,
        String status
) {
}
