package com.anysale.billing.application;

import com.anysale.billing.api.TenantSubscriptionResponse;
import com.anysale.billing.config.AsaasBillingProperties;
import com.anysale.billing.domain.BillingWebhookEvent;
import com.anysale.billing.domain.TenantSubscription;
import com.anysale.billing.persistence.BillingWebhookEventRepository;
import com.anysale.billing.persistence.BillingPlanRepository;
import com.anysale.billing.persistence.TenantSubscriptionRepository;
import com.anysale.billing.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantBillingService {
    private static final String PROVIDER = "ASAAS";
    private final TenantSubscriptionRepository subscriptions;
    private final BillingWebhookEventRepository events;
    private final BillingPlanRepository plans;
    private final TenantContext tenants;
    private final AsaasBillingProperties asaas;
    private final ObjectMapper mapper;
    public TenantBillingService(TenantSubscriptionRepository subscriptions, BillingWebhookEventRepository events,
                                BillingPlanRepository plans, TenantContext tenants, AsaasBillingProperties asaas, ObjectMapper mapper) {
        this.subscriptions=subscriptions; this.events=events; this.plans=plans; this.tenants=tenants; this.asaas=asaas; this.mapper=mapper;
    }
    @Transactional(readOnly = true)
    public List<com.anysale.billing.api.BillingPlanResponse> availablePlans() {
        return plans.findByActiveTrueOrderByMonthlyPriceCentsAsc().stream().map(plan -> new com.anysale.billing.api.BillingPlanResponse(
                plan.getCode(), plan.getName(), plan.getDescription(), plan.getMonthlyPriceCents(), plan.getUserLimit(),
                plan.getMonthlyLeadLimit(), plan.getMonthlyAiRequestLimit(), plan.getTrialDays(), plan.getGraceDays())).toList();
    }
    @Transactional(readOnly = true)
    public TenantSubscriptionResponse currentSubscription() {
        String tenant = tenants.tenantId();
        return subscriptions.findById(tenant).map(this::response)
                .orElse(new TenantSubscriptionResponse(tenant, null, "NOT_CONFIGURED", "SETUP_REQUIRED", null, null));
    }
    @Transactional
    public void receiveAsaasWebhook(String token, JsonNode payload) {
        ensureToken(token);
        String eventId=text(payload,"id"), type=text(payload,"event");
        if (!StringUtils.hasText(eventId) || !StringUtils.hasText(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evento do Asaas inválido.");
        if (events.findByProviderAndProviderEventId(PROVIDER,eventId).isPresent()) return;
        BillingWebhookEvent event=new BillingWebhookEvent();
        event.setProvider(PROVIDER); event.setProviderEventId(eventId); event.setEventType(type); event.setPayload(json(payload)); event.setProcessingResult("IGNORED");
        String externalSubscription=subscriptionId(payload);
        Optional<TenantSubscription> subscription=externalSubscription==null ? Optional.empty() : subscriptions.findByProviderAndProviderSubscriptionId(PROVIDER,externalSubscription);
        subscription.ifPresent(value -> { apply(value,type); subscriptions.save(value); event.setProcessingResult("APPLIED"); });
        event.setProcessedAt(Instant.now()); events.save(event);
    }
    private TenantSubscriptionResponse response(TenantSubscription s) { return new TenantSubscriptionResponse(s.getTenantId(),s.getPlanCode(),s.getStatus(),access(s),s.getTrialEndsAt(),s.getCurrentPeriodEndsAt()); }
    private String access(TenantSubscription s) {
        if ("ACTIVE".equals(s.getStatus())) return "ACTIVE";
        if ("TRIALING".equals(s.getStatus()) && (s.getTrialEndsAt()==null || s.getTrialEndsAt().isAfter(Instant.now()))) return "ACTIVE";
        return "PAST_DUE".equals(s.getStatus()) ? "GRACE_PERIOD" : "BLOCKED";
    }
    private void apply(TenantSubscription s,String event) { switch(event) {
        case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> s.setStatus("ACTIVE");
        case "PAYMENT_OVERDUE", "PAYMENT_DUNNING_REQUESTED" -> s.setStatus("PAST_DUE");
        case "SUBSCRIPTION_INACTIVATED", "SUBSCRIPTION_DELETED" -> s.setStatus("CANCELLED");
        default -> { return; }
    }}
    private String subscriptionId(JsonNode payload) { String id=payload.path("payment").path("subscription").asText(null); return StringUtils.hasText(id)?id:payload.path("subscription").path("id").asText(null); }
    private void ensureToken(String token) {
        if(!asaas.enabled()||!StringUtils.hasText(asaas.webhookToken())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"O recebimento de eventos de cobrança ainda não está configurado.");
        if(!asaas.webhookToken().equals(token)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Token de webhook inválido.");
    }
    private String text(JsonNode payload,String field){return payload.path(field).asText(null);}
    private String json(JsonNode payload){try{return mapper.writeValueAsString(payload);}catch(JsonProcessingException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evento de cobrança inválido.");}}
}
