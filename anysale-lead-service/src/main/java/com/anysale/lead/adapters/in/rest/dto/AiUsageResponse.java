package com.anysale.lead.adapters.in.rest.dto;

public record AiUsageResponse(String month, long requests, long inputTokens, long outputTokens, long totalTokens,
                              Integer monthlyRequestLimit, Long monthlyTokenLimit) { }
