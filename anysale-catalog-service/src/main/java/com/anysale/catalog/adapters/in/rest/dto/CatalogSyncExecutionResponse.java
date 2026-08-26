package com.anysale.catalog.adapters.in.rest.dto;

import java.time.Instant;
import java.util.List;

public record CatalogSyncExecutionResponse(
        String id,
        String tenantId,
        String integrationId,
        String status,
        Instant startedAt,
        Instant finishedAt,
        int createdCount,
        int updatedCount,
        int skippedCount,
        int errorCount,
        List<String> errorSummary
) {}
