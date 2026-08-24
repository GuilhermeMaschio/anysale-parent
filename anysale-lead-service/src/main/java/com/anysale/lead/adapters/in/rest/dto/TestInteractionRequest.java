package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Local-only payload used to build a conversation before testing AI enrichment.
 */
public record TestInteractionRequest(
        @NotBlank String message,
        @Pattern(regexp = "IN|OUT") String direction
) {
}
