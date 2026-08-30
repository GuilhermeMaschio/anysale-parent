package com.anysale.catalog.adapters.in.rest.dto;

import java.time.Instant;
import java.util.Map;

public record CatalogIntegrationResponse(
        String id,
        String tenantId,
        String name,
        String providerType,
        String baseUrl,
        String status,
        boolean hasCredentials,
        String authType,
        String apiKeyHeaderName,
        String apiKeyQueryName,
        String syncMode,
        String schedule,
        Map<String, String> fieldMapping,
        String conflictStrategy,
        boolean skuFallbackEnabled,
        Instant lastSyncAt,
        String lastSyncStatus,
        Instant createdAt,
        Instant updatedAt
) {}
