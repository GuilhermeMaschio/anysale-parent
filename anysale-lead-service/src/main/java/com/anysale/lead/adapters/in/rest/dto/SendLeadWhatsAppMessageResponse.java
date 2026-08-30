package com.anysale.lead.adapters.in.rest.dto;

import java.util.UUID;

public record SendLeadWhatsAppMessageResponse(
        UUID leadId,
        String to,
        String waId,
        String messageId,
        String status
) {
}
