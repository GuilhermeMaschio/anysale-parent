package com.anysale.billing.application;

import com.anysale.billing.api.TenantSubscriptionResponse;
import com.anysale.billing.api.BillingCheckoutResponse;
import com.anysale.billing.domain.BillingCheckout;
import com.anysale.billing.config.AsaasBillingProperties;
import com.anysale.billing.domain.BillingWebhookEvent;
import com.anysale.billing.domain.TenantSubscription;
import com.anysale.billing.persistence.BillingWebhookEventRepository;
import com.anysale.billing.persistence.BillingPlanRepository;
import com.anysale.billing.persistence.BillingCheckoutRepository;
import com.anysale.billing.persistence.TenantSubscriptionRepository;
import com.anysale.billing.tenant.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
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
    private final BillingCheckoutRepository checkouts;
    private final TenantContext tenants;
    private final AsaasBillingProperties asaas;
    private final ObjectMapper mapper;
    private final AsaasCheckoutClient checkoutClient;
    public TenantBillingService(TenantSubscriptionRepository subscriptions, BillingWebhookEventRepository events,
                                BillingPlanRepository plans, BillingCheckoutRepository checkouts, TenantContext tenants,
                                AsaasBillingProperties asaas, ObjectMapper mapper, AsaasCheckoutClient checkoutClient) {
        this.subscriptions=subscriptions; this.events=events; this.plans=plans; this.checkouts=checkouts; this.tenants=tenants; this.asaas=asaas; this.mapper=mapper; this.checkoutClient=checkoutClient;
    }
    @Transactional(readOnly = true)
    public List<com.anysale.billing.api.BillingPlanResponse> availablePlans() {
        return plans.findByActiveTrueOrderByMonthlyPriceCentsAsc().stream().map(plan -> new com.anysale.billing.api.BillingPlanResponse(
                plan.getCode(), plan.getName(), plan.getDescription(), plan.getMonthlyPriceCents(), plan.getUserLimit(),
                plan.getMonthlyLeadLimit(), plan.getMonthlyAiRequestLimit(), plan.getTrialDays(), plan.getGraceDays())).toList();
    }
    @Transactional
    public BillingCheckoutResponse startCheckout(String planCode) {
        com.anysale.billing.domain.BillingPlan plan = plans.findById(planCode.trim().toUpperCase())
                .filter(com.anysale.billing.domain.BillingPlan::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado."));
        if (plan.getMonthlyPriceCents() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este plano requer uma proposta comercial.");
        String tenant = tenants.tenantId();
        subscriptions.findById(tenant).ifPresent(current -> {
            if ("ACTIVE".equals(current.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "A empresa já possui uma assinatura ativa.");
            if ("CHECKOUT_PENDING".equals(current.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um pagamento aguardando conclusão. Finalize-o ou aguarde sua expiração antes de tentar novamente.");
        });
        Instant firstDue = Instant.now().plus(java.time.Duration.ofDays(plan.getTrialDays()));
        String externalReference = "billing:" + tenant + ":" + UUID.randomUUID();
        AsaasCheckoutClient.CheckoutSession session = checkoutClient.create(plan, externalReference, firstDue);
        TenantSubscription subscription = subscriptions.findById(tenant).orElseGet(TenantSubscription::new);
        subscription.setTenantId(tenant); subscription.setProvider(PROVIDER); subscription.setPlanCode(plan.getCode()); subscription.setStatus("CHECKOUT_PENDING"); subscription.setTrialEndsAt(firstDue); subscriptions.save(subscription);
        BillingCheckout checkout = new BillingCheckout();
        checkout.setTenantId(tenant); checkout.setPlanCode(plan.getCode()); checkout.setProvider(PROVIDER); checkout.setProviderCheckoutId(session.id()); checkout.setExternalReference(externalReference); checkout.setCheckoutUrl(session.link()); checkout.setStatus("ACTIVE"); checkout.setExpiresAt(Instant.now().plus(java.time.Duration.ofMinutes(1440))); checkouts.save(checkout);
        return new BillingCheckoutResponse(session.link(), checkout.getExpiresAt(), plan.getCode());
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
        if ("SUBSCRIPTION_CREATED".equals(type)) {
            String reference = payload.path("subscription").path("externalReference").asText(null);
            if (StringUtils.hasText(reference) && StringUtils.hasText(externalSubscription)) {
                checkouts.findByExternalReference(reference).ifPresent(checkout -> subscriptions.findById(checkout.getTenantId()).ifPresent(value -> {
                    value.setProviderSubscriptionId(externalSubscription);
                    subscriptions.save(value);
                    event.setProcessingResult("APPLIED");
                }));
            }
        }
        checkoutId(payload).flatMap(id -> checkouts.findByProviderAndProviderCheckoutId(PROVIDER,id)).ifPresent(checkout -> {
            checkout.setStatus(checkoutStatus(type)); checkouts.save(checkout);
            if ("CHECKOUT_PAID".equals(type)) subscriptions.findById(checkout.getTenantId()).ifPresent(value -> { value.setStatus("ACTIVE"); subscriptions.save(value); });
            if ("CHECKOUT_CANCELED".equals(type) || "CHECKOUT_EXPIRED".equals(type)) subscriptions.findById(checkout.getTenantId())
                    .filter(value -> "CHECKOUT_PENDING".equals(value.getStatus()) && checkout.getPlanCode().equals(value.getPlanCode()))
                    .ifPresent(value -> { value.setStatus("CHECKOUT_" + ("CHECKOUT_CANCELED".equals(type) ? "CANCELLED" : "EXPIRED")); subscriptions.save(value); });
            event.setProcessingResult("APPLIED");
        });
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
    private Optional<String> checkoutId(JsonNode payload) { String id=payload.path("checkout").path("id").asText(null); return StringUtils.hasText(id)?Optional.of(id):Optional.empty(); }
    private String checkoutStatus(String event) { return switch (event) { case "CHECKOUT_PAID" -> "PAID"; case "CHECKOUT_CANCELED" -> "CANCELLED"; case "CHECKOUT_EXPIRED" -> "EXPIRED"; default -> "ACTIVE"; }; }
    private void ensureToken(String token) {
        if(!asaas.enabled()||!StringUtils.hasText(asaas.webhookToken())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"O recebimento de eventos de cobrança ainda não está configurado.");
        if(!asaas.webhookToken().equals(token)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Token de webhook inválido.");
    }
    private String text(JsonNode payload,String field){return payload.path(field).asText(null);}
    private String json(JsonNode payload){try{return mapper.writeValueAsString(payload);}catch(JsonProcessingException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evento de cobrança inválido.");}}
}
