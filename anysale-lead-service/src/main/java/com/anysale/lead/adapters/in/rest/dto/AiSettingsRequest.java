package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiSettingsRequest(
        boolean enabled,
        @Size(max = 120) String model,
        @Min(100) @Max(4000) int maxOutputTokens,
        @Min(1) Integer monthlyRequestLimit,
        @Min(1) Long monthlyTokenLimit,
        @jakarta.validation.constraints.Pattern(regexp = "CONSULTATIVE|DIRECT|PREMIUM|REACTIVATION") String serviceProfile,
        @jakarta.validation.constraints.Pattern(regexp = "WARM|NEUTRAL|TECHNICAL") String tone,
        @jakarta.validation.constraints.Pattern(regexp = "INFORMAL|BALANCED|FORMAL") String formality,
        @jakarta.validation.constraints.Pattern(regexp = "CONCISE|BALANCED|DETAILED") String responseLength,
        @jakarta.validation.constraints.Pattern(regexp = "DISCOVER_FIRST|OFFER_WHEN_FIT|REACTIVATE") String commercialApproach,
        @Size(max = 3000) String customInstructions,
        @Size(max = 4000) String approvedExamples,
        @Size(max = 4000) String rejectedExamples
) { }
