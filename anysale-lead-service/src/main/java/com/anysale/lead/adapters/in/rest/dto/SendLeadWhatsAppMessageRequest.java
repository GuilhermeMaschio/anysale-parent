package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendLeadWhatsAppMessageRequest(
        @NotBlank @Size(max = 2000) String message
) {
}
