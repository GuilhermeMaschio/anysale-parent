package com.anysale.billing.api;

import java.time.Instant;

public record TenantSubscriptionResponse(String tenantId, String planCode, String status, String accessStatus,
                                         Instant trialEndsAt, Instant currentPeriodEndsAt) { }
