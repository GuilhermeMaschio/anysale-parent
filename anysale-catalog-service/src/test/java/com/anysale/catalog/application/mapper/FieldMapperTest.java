package com.anysale.catalog.application.mapper;

import com.anysale.catalog.domain.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FieldMapperTest {

    private FieldMapper fieldMapper;

    @BeforeEach
    void setUp() {
        fieldMapper = new FieldMapper();
    }

    @Test
    void shouldMapRawDataToProductUsingFieldMapping() {
        Map<String, Object> rawData = new HashMap<>();
        rawData.put("code", "PROD-001");
        rawData.put("name", "Smartphone Galaxy");
        rawData.put("price_cents", 1999.90);
        rawData.put("stock_lvl", 15);
        rawData.put("img", "https://example.com/phone.jpg");

        Map<String, String> mapping = Map.of(
                "sku", "code",
                "title", "name",
                "price", "price_cents",
                "stockQuantity", "stock_lvl",
                "imageUrl", "img"
        );

        FieldMapper.MappingResult result = fieldMapper.mapToProduct(rawData, mapping, "tenant-1", "integration-1");

        assertTrue(result.isValid());
        assertTrue(result.validationErrors().isEmpty());

        Product product = result.product();
        assertEquals("PROD-001", product.getSku());
        assertEquals("Smartphone Galaxy", product.getTitle());
        assertEquals(0, new BigDecimal("1999.90").compareTo(product.getPrice()));
        assertEquals(15, product.getStockQuantity());
        assertEquals("tenant-1", product.getTenantId());
        assertEquals("integration-1", product.getSource());
        assertEquals("https://example.com/phone.jpg", result.imageUrl());
    }

    @Test
    void shouldFailValidationWhenRequiredFieldsAreMissing() {
        Map<String, Object> rawData = Map.of("category", "Eletrônicos");
        Map<String, String> mapping = Map.of();

        FieldMapper.MappingResult result = fieldMapper.mapToProduct(rawData, mapping, "tenant-1", "integration-1");

        assertFalse(result.isValid());
        assertTrue(result.validationErrors().contains("Missing required field: title"));
        assertTrue(result.validationErrors().contains("Missing or invalid required field: price"));
    }
}
