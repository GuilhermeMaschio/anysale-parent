package com.anysale.catalog.adapters.in.rest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank @Size(max = 64) String sku,
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 80) String category,
        @Size(max = 1000) String description,
        @Size(max = 3) String currency,
        @Size(max = 120) String vendor,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        List<@Size(max = 48) String> tags,
        boolean available,
        @Min(0) int reorderPoint,
        @Min(0) int initialStock
) { }
