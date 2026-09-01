package com.anysale.billing.api;
import java.time.Instant;
public record BillingCheckoutResponse(String checkoutUrl, Instant expiresAt, String planCode) { }
