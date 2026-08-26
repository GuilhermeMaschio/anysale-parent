package com.anysale.catalog.adapters.in.rest.dto;

public record TestConnectionResponse(
        boolean success,
        int statusCode,
        String message
) {}
