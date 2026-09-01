package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.TenantSubscriptionResponse;
import com.anysale.lead.adapters.out.persistence.BillingWebhookEventJpaRepository;
import com.anysale.lead.adapters.out.persistence.TenantSubscriptionJpaRepository;
import com.anysale.lead.config.AsaasBillingProperties;
import com.anysale.lead.domain.model.BillingWebhookEvent;
import com.anysale.lead.domain.model.TenantSubscription;
import com.anysale.lead.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Tenant billing state. Provider events are recorded before being applied so retries are safe. */
@Service
public class TenantBillingService {
    private static final String PROVIDER = "ASAAS";
    private final TenantSubscriptionJpaRepository subscriptions;
    private final BillingWebhookEventJpaRepository events;
    private final TenantContext tenants;
    private final AsaasBillingProperties asaas;
    private final ObjectMapper objectMapper;

    public TenantBillingService(TenantSubscriptionJpaRepository subscriptions, BillingWebhookEventJpaRepository events,
                                TenantContext tenants, AsaasBillingProperties asaas, ObjectMapper objectMapper) {
        this.subscriptions = subscriptions;
        this.events = events;
        this.tenants = tenants;
        this.asaas = asaas;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TenantSubscriptionResponse currentSubscription() {
        String tenantId = tenants.tenantId();
        return subscriptions.findById(tenantId).map(this::response)
                .orElse(new TenantSubscriptionResponse(tenantId, null, "NOT_CONFIGURED", "SETUP_REQUIRED", null, null));
    }

    /**
     * Registers the event exactly once. A provider event alone never creates a new tenant:
     * the checkout flow must first persist the provider subscription relation.
     */
    @Transactional
    public void receiveAsaasWebhook(String providedToken, JsonNode payload) {
        ensureWebhookConfigured(providedToken);
        String eventId = text(payload, "id");
        String eventType = text(payload, "event");
        if (!StringUtils.hasText(eventId) || !StringUtils.hasText(eventType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento do Asaas inválido.");
        }
        if (events.findByProviderAndProviderEventId(PROVIDER, eventId).isPresent()) return;

        BillingWebhookEvent event = new BillingWebhookEvent();
        event.setProvider(PROVIDER);
        event.setProviderEventId(eventId);
        event.setEventType(eventType);
        event.setPayload(serialize(payload));
        event.setProcessingResult("IGNORED");

        String providerSubscriptionId = subscriptionId(payload);
        Optional<TenantSubscription> subscription = providerSubscriptionId == null ? Optional.empty()
                : subscriptions.findByProviderAndProviderSubscriptionId(PROVIDER, providerSubscriptionId);
        subscription.ifPresent(value -> {
            applyPaymentState(value, eventType);
            subscriptions.save(value);
            event.setProcessingResult("APPLIED");
        });
        event.setProcessedAt(Instant.now());
        events.save(event);
    }

    private TenantSubscriptionResponse response(TenantSubscription subscription) {
        return new TenantSubscriptionResponse(subscription.getTenantId(), subscription.getPlanCode(), subscription.getStatus(),
                accessStatus(subscription), subscription.getTrialEndsAt(), subscription.getCurrentPeriodEndsAt());
    }

    private String accessStatus(TenantSubscription subscription) {
        if ("ACTIVE".equals(subscription.getStatus())) return "ACTIVE";
        if ("TRIALING".equals(subscription.getStatus()) && (subscription.getTrialEndsAt() == null || subscription.getTrialEndsAt().isAfter(Instant.now()))) return "ACTIVE";
        if ("PAST_DUE".equals(subscription.getStatus())) return "GRACE_PERIOD";
        return "BLOCKED";
    }

    private void applyPaymentState(TenantSubscription subscription, String eventType) {
        switch (eventType) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> subscription.setStatus("ACTIVE");
            case "PAYMENT_OVERDUE", "PAYMENT_DUNNING_REQUESTED" -> subscription.setStatus("PAST_DUE");
            case "SUBSCRIPTION_INACTIVATED", "SUBSCRIPTION_DELETED" -> subscription.setStatus("CANCELLED");
            default -> { return; }
        }
    }

    private String subscriptionId(JsonNode payload) {
        String paymentSubscription = payload.path("payment").path("subscription").asText(null);
        if (StringUtils.hasText(paymentSubscription)) return paymentSubscription;
        String subscriptionId = payload.path("subscription").path("id").asText(null);
        return StringUtils.hasText(subscriptionId) ? subscriptionId : null;
    }

    private void ensureWebhookConfigured(String providedToken) {
        if (!asaas.enabled() || !StringUtils.hasText(asaas.webhookToken())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "O recebimento de eventos de cobrança ainda não está configurado.");
        }
        if (!asaas.webhookToken().equals(providedToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de webhook inválido.");
        }
    }

    private String text(JsonNode payload, String field) { return payload.path(field).asText(null); }
    private String serialize(JsonNode payload) {
        try { return objectMapper.writeValueAsString(payload); }
        catch (JsonProcessingException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento de cobrança inválido."); }
    }
}
