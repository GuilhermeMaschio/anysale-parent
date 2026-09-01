package com.anysale.billing.application;

import com.anysale.billing.config.AsaasBillingProperties;
import com.anysale.billing.domain.BillingPlan;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** Asaas-hosted checkout keeps payment-card data out of AnySale. */
@Component
public class AsaasCheckoutClient {
    public record CheckoutSession(String id, String link) { }
    private final AsaasBillingProperties properties;
    private final RestClient client = RestClient.create();
    public AsaasCheckoutClient(AsaasBillingProperties properties) { this.properties = properties; }

    public CheckoutSession create(BillingPlan plan, String externalReference, Instant firstDueAt) {
        ensureConfigured();
        Map<String, Object> payload = Map.of(
                "billingTypes", List.of("CREDIT_CARD"),
                "chargeTypes", List.of("RECURRENT"),
                "minutesToExpire", 1440,
                "externalReference", externalReference,
                "callback", Map.of("successUrl", properties.checkoutSuccessUrl(), "cancelUrl", properties.checkoutCancelUrl(), "expiredUrl", properties.checkoutExpiredUrl()),
                "items", List.of(Map.of("name", "AnySale " + plan.getName(), "description", plan.getDescription(), "quantity", 1, "value", price(plan))),
                "subscription", Map.of("cycle", "MONTHLY", "nextDueDate", format(firstDueAt))
        );
        try {
            AsaasResponse response = client.post().uri(properties.baseUrl() + "/checkouts")
                    .header("access_token", properties.apiKey()).header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(payload).retrieve().body(AsaasResponse.class);
            if (response == null || !StringUtils.hasText(response.id()) || !StringUtils.hasText(response.link())) {
                throw unavailable();
            }
            return new CheckoutSession(response.id(), response.link());
        } catch (RestClientException exception) { throw unavailable(); }
    }

    private BigDecimal price(BillingPlan plan) { return BigDecimal.valueOf(plan.getMonthlyPriceCents()).movePointLeft(2); }
    private String format(Instant value) { return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(value); }
    private void ensureConfigured() {
        if (!properties.enabled() || !StringUtils.hasText(properties.apiKey()) || !StringUtils.hasText(properties.baseUrl())
                || !StringUtils.hasText(properties.checkoutSuccessUrl()) || !StringUtils.hasText(properties.checkoutCancelUrl()) || !StringUtils.hasText(properties.checkoutExpiredUrl())) throw unavailable();
    }
    private ResponseStatusException unavailable() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O checkout Asaas ainda não está configurado."); }
    private record AsaasResponse(String id, String link) { }
}
