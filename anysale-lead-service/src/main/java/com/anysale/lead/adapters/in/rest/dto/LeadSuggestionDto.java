package com.anysale.lead.adapters.in.rest.dto;

import com.anysale.lead.domain.model.LeadSuggestion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class LeadSuggestionDto {
    private UUID id;
    private String productId;
    private String title;
    private BigDecimal price;
    private String currency;
    private String vendor;
    private Instant createdAt;
}