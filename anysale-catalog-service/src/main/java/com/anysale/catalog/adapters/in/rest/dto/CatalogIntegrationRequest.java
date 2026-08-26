package com.anysale.catalog.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record CatalogIntegrationRequest(
        @NotBlank(message = "Name is required") String name,
        String providerType,
        @NotBlank(message = "Base URL is required") String baseUrl,
        String status,
        String secret, // Plain text credential (API Key or Bearer Token) sent by client; will be encrypted
        String authType,
        String apiKeyHeaderName,
        String apiKeyQueryName,
        String syncMode,
        String schedule,
        Map<String, String> fieldMapping,
        String conflictStrategy,
        Boolean skuFallbackEnabled
) {}
