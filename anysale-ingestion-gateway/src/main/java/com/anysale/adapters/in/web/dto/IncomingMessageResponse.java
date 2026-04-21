package com.anysale.adapters.in.web.dto;

import com.anysale.application.model.LeadSnapshot;

import java.util.UUID;

public record IncomingMessageResponse(
        String status,
        String normalizedPhone,
        UUID leadId,
        LeadSnapshot lead
) {
}
