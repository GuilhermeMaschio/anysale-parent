package com.anysale.lead.adapters.in.rest.dto;

import java.time.Instant;
import java.util.List;

public record AiSettingsResponse(
        boolean enabled, boolean providerAvailable, String model, int maxOutputTokens,
        Integer monthlyRequestLimit, Long monthlyTokenLimit, List<String> allowedModels, Instant updatedAt
) { }
