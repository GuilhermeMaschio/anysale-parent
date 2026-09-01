package com.anysale.billing.api;

public record BillingPlanResponse(String code, String name, String description, Integer monthlyPriceCents,
                                  Integer userLimit, Integer monthlyLeadLimit, Integer monthlyAiRequestLimit,
                                  int trialDays, int graceDays) { }
