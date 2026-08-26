package com.anysale.catalog.application.mapper;

import com.anysale.catalog.domain.model.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class FieldMapper {

    public record MappingResult(
            Product product,
            String imageUrl,
            boolean isValid,
            List<String> validationErrors
    ) {}

    public MappingResult mapToProduct(Map<String, Object> rawData, Map<String, String> fieldMapping, String tenantId, String source) {
        Product product = new Product();
        product.setTenantId(tenantId);
        product.setSource(source);

        List<String> errors = new ArrayList<>();
        Map<String, String> mapping = fieldMapping != null ? fieldMapping : Collections.emptyMap();

        String externalProductId = extractString(rawData, mapping.getOrDefault("externalProductId", "id"));
        if (externalProductId == null || externalProductId.isBlank()) {
            // Fallback to sku or generated
            externalProductId = extractString(rawData, mapping.getOrDefault("sku", "sku"));
        }
        product.setExternalProductId(externalProductId);

        String sku = extractString(rawData, mapping.getOrDefault("sku", "sku"));
        if (sku == null || sku.isBlank()) {
            sku = externalProductId;
        }
        if (sku == null || sku.isBlank()) {
            errors.add("Missing required field: sku");
        }
        product.setSku(sku);

        String title = extractString(rawData, mapping.getOrDefault("title", "title"));
        if (title == null || title.isBlank()) {
            title = extractString(rawData, mapping.getOrDefault("name", "name"));
        }
        if (title == null || title.isBlank()) {
            errors.add("Missing required field: title");
        }
        product.setTitle(title);

        String description = extractString(rawData, mapping.getOrDefault("description", "description"));
        product.setDescription(description);

        String category = extractString(rawData, mapping.getOrDefault("category", "category"));
        product.setCategory(category != null ? category : "Geral");

        String currency = extractString(rawData, mapping.getOrDefault("currency", "currency"));
        product.setCurrency(currency != null && !currency.isBlank() ? currency : "BRL");

        BigDecimal price = extractBigDecimal(rawData, mapping.getOrDefault("price", "price"));
        if (price == null) {
            errors.add("Missing or invalid required field: price");
        } else {
            product.setPrice(price);
        }

        Integer stockQuantity = extractInteger(rawData, mapping.getOrDefault("stockQuantity", "stockQuantity"));
        if (stockQuantity == null) {
            stockQuantity = extractInteger(rawData, mapping.getOrDefault("stock", "stock"));
        }
        product.setStockQuantity(stockQuantity != null ? stockQuantity : 0);

        Integer reorderPoint = extractInteger(rawData, mapping.getOrDefault("reorderPoint", "reorderPoint"));
        product.setReorderPoint(reorderPoint != null ? reorderPoint : 0);

        Boolean available = extractBoolean(rawData, mapping.getOrDefault("available", "available"));
        product.setAvailable(available != null ? available : true);

        String imageUrl = extractString(rawData, mapping.getOrDefault("imageUrl", "imageUrl"));
        if (imageUrl == null) {
            imageUrl = extractString(rawData, mapping.getOrDefault("image", "image"));
        }

        boolean isValid = errors.isEmpty();
        return new MappingResult(product, imageUrl, isValid, errors);
    }

    private String extractString(Map<String, Object> data, String path) {
        Object val = extractByPath(data, path);
        return val != null ? String.valueOf(val).trim() : null;
    }

    private BigDecimal extractBigDecimal(Map<String, Object> data, String path) {
        Object val = extractByPath(data, path);
        if (val == null) return null;
        if (val instanceof Number num) {
            return BigDecimal.valueOf(num.doubleValue());
        }
        try {
            String str = String.valueOf(val).replaceAll("[^0-9.,]", "").replace(",", ".");
            return new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractInteger(Map<String, Object> data, String path) {
        Object val = extractByPath(data, path);
        if (val == null) return null;
        if (val instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean extractBoolean(Map<String, Object> data, String path) {
        Object val = extractByPath(data, path);
        if (val == null) return null;
        if (val instanceof Boolean b) return b;
        String str = String.valueOf(val).trim().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str) || "active".equals(str);
    }

    @SuppressWarnings("unchecked")
    private Object extractByPath(Map<String, Object> data, String path) {
        if (data == null || path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else {
                return null;
            }
        }
        return current;
    }
}
