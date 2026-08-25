package com.anysale.catalog.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductResponse(String id, String sku, String title, String category, String description,
                              String currency, String vendor, BigDecimal price, List<String> tags,
                              boolean available, int stockQuantity, int reservedQuantity, int availableQuantity,
                              int reorderPoint, boolean lowStock, boolean hasImage, Instant updatedAt) { }
