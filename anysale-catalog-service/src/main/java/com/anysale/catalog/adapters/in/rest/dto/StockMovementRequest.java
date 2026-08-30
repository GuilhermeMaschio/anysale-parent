package com.anysale.catalog.adapters.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StockMovementRequest(
        @Pattern(regexp = "IN|OUT") String type,
        @Min(1) int quantity,
        @NotBlank @Size(max = 240) String reason
) { }
