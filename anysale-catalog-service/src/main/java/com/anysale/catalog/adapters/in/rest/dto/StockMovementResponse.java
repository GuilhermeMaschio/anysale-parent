package com.anysale.catalog.adapters.in.rest.dto;

import java.time.Instant;
public record StockMovementResponse(String id, String type, int quantity, int balanceAfter, String reason, String createdBy, Instant createdAt) { }
