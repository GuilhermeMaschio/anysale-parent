package com.anysale.lead.adapters.in.rest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class BulkApplyResponseDto {
    private UUID leadId;
    private int applied;
    private int skipped;
    private int errors;
    private Instant updatedAt;
    private List<ItemResult> items;

    @Data
    @Builder
    public static class ItemResult {
        private String status; // APPLIED | SKIPPED_DUPLICATE | ERROR
        private LeadSuggestionDto suggestion;
        private String message; // opcional
    }
}
