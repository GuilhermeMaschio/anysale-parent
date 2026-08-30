package com.anysale.catalog.application.connector;

public record ConnectionTestResult(
        boolean success,
        int statusCode,
        String message
) {}
