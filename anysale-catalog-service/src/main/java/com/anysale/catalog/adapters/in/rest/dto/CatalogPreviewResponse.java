package com.anysale.catalog.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record CatalogPreviewResponse(
        int totalItems,
        List<PreviewItem> items
) {
    public record PreviewItem(
            String externalProductId,
            String sku,
            String title,
            String category,
            String description,
            BigDecimal price,
            String currency,
            int stockQuantity,
            int reorderPoint,
            boolean available,
            String imageUrl,
            boolean isValid,
            List<String> validationErrors
    ) {}
}
