package com.anysale.billing.api;
import jakarta.validation.constraints.NotBlank;
public record BillingCheckoutRequest(@NotBlank String planCode) { }
