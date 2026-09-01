package com.anysale.lead.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anysale.billing.asaas")
public record AsaasBillingProperties(boolean enabled, String apiKey, String webhookToken) { }
