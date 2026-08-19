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
        @Min(1) Long monthlyTokenLimit
) { }
