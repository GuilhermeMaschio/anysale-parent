package com.anysale.lead.adapters.out.notification.dto;

import java.util.UUID;

public record SendWhatsAppMessageResponse(
        UUID leadId,
        String to,
        String waId,
        String messageId,
        String status
) {
}
