package com.anysale.lead.adapters.out.notification.dto;

import java.util.UUID;

public record SendWhatsAppMessageRequest(UUID leadId, String to, String message) {
}
