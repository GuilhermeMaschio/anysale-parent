package com.anysale.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anysale.billing.config.AsaasBillingProperties;
import com.anysale.billing.domain.BillingWebhookEvent;
import com.anysale.billing.domain.BillingCheckout;
import com.anysale.billing.domain.TenantSubscription;
import com.anysale.billing.domain.BillingPlan;
import com.anysale.billing.api.BillingCheckoutResponse;
import com.anysale.billing.persistence.BillingWebhookEventRepository;
import com.anysale.billing.persistence.BillingPlanRepository;
import com.anysale.billing.persistence.BillingCheckoutRepository;
import com.anysale.billing.persistence.TenantSubscriptionRepository;
import com.anysale.billing.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantBillingServiceTest {
    @Mock TenantSubscriptionRepository subscriptions;
    @Mock BillingWebhookEventRepository events;
    @Mock BillingPlanRepository plans;
    @Mock BillingCheckoutRepository checkouts;
    @Mock AsaasCheckoutClient checkoutClient;
    @Mock TenantContext tenants;
    @Test
    void confirmsOnlyTheMappedTenantSubscription() throws Exception {
        TenantSubscription subscription=new TenantSubscription(); subscription.setStatus("PAST_DUE");
        when(events.findByProviderAndProviderEventId("ASAAS","evt-1")).thenReturn(Optional.empty());
        when(subscriptions.findByProviderAndProviderSubscriptionId("ASAAS","sub-1")).thenReturn(Optional.of(subscription));
        service().receiveAsaasWebhook("token", new ObjectMapper().readTree("""
                {"id":"evt-1","event":"PAYMENT_RECEIVED","payment":{"subscription":"sub-1"}}
                """));
        assertThat(subscription.getStatus()).isEqualTo("ACTIVE"); verify(subscriptions).save(subscription);
        ArgumentCaptor<BillingWebhookEvent> event=ArgumentCaptor.forClass(BillingWebhookEvent.class); verify(events).save(event.capture());
        assertThat(event.getValue().getProcessingResult()).isEqualTo("APPLIED");
    }
    @Test
    void createsHostedCheckoutWithoutCollectingCardData() {
        BillingPlan plan=new BillingPlan(); plan.setCode("ESSENTIAL"); plan.setName("Essencial"); plan.setDescription("Plano inicial"); plan.setMonthlyPriceCents(14900); plan.setTrialDays(14); plan.setActive(true);
        when(plans.findById("ESSENTIAL")).thenReturn(Optional.of(plan)); when(tenants.tenantId()).thenReturn("tenant-a"); when(subscriptions.findById("tenant-a")).thenReturn(Optional.empty());
        when(checkoutClient.create(org.mockito.ArgumentMatchers.eq(plan),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(new AsaasCheckoutClient.CheckoutSession("checkout-1","https://asaas.example/checkout-1"));
        BillingCheckoutResponse response=service().startCheckout("essential");
        assertThat(response.checkoutUrl()).isEqualTo("https://asaas.example/checkout-1"); verify(checkouts).save(org.mockito.ArgumentMatchers.any()); verify(subscriptions).save(org.mockito.ArgumentMatchers.any());
    }
    @Test
    void mapsCreatedProviderSubscriptionBackToTheTenantCheckout() throws Exception {
        BillingCheckout checkout = new BillingCheckout(); checkout.setTenantId("tenant-a");
        TenantSubscription subscription = new TenantSubscription();
        when(events.findByProviderAndProviderEventId("ASAAS", "evt-2")).thenReturn(Optional.empty());
        when(checkouts.findByExternalReference("billing:tenant-a:abc")).thenReturn(Optional.of(checkout));
        when(subscriptions.findById("tenant-a")).thenReturn(Optional.of(subscription));
        service().receiveAsaasWebhook("token", new ObjectMapper().readTree("""
                {"id":"evt-2","event":"SUBSCRIPTION_CREATED","subscription":{"id":"sub-2","externalReference":"billing:tenant-a:abc"}}
                """));
        assertThat(subscription.getProviderSubscriptionId()).isEqualTo("sub-2");
        verify(subscriptions).save(subscription);
    }
    @Test
    void releasesTheTenantForANewCheckoutWhenThePreviousOneExpires() throws Exception {
        BillingCheckout checkout = new BillingCheckout(); checkout.setTenantId("tenant-a"); checkout.setPlanCode("ESSENTIAL");
        TenantSubscription subscription = new TenantSubscription(); subscription.setPlanCode("ESSENTIAL"); subscription.setStatus("CHECKOUT_PENDING");
        when(events.findByProviderAndProviderEventId("ASAAS", "evt-3")).thenReturn(Optional.empty());
        when(checkouts.findByProviderAndProviderCheckoutId("ASAAS", "checkout-1")).thenReturn(Optional.of(checkout));
        when(subscriptions.findById("tenant-a")).thenReturn(Optional.of(subscription));
        service().receiveAsaasWebhook("token", new ObjectMapper().readTree("""
                {"id":"evt-3","event":"CHECKOUT_EXPIRED","checkout":{"id":"checkout-1"}}
                """));
        assertThat(subscription.getStatus()).isEqualTo("CHECKOUT_EXPIRED");
        verify(subscriptions).save(subscription);
    }
    @Test
    void returnsTheExistingCheckoutWhileTheTenantIsStillAwaitingPayment() {
        BillingCheckout checkout = new BillingCheckout(); checkout.setTenantId("tenant-a"); checkout.setPlanCode("ESSENTIAL"); checkout.setCheckoutUrl("https://asaas.example/checkout-1"); checkout.setStatus("ACTIVE"); checkout.setExpiresAt(Instant.now().plusSeconds(600));
        TenantSubscription subscription = new TenantSubscription(); subscription.setStatus("CHECKOUT_PENDING");
        when(tenants.tenantId()).thenReturn("tenant-a"); when(subscriptions.findById("tenant-a")).thenReturn(Optional.of(subscription));
        when(checkouts.findFirstByTenantIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.eq("tenant-a"), org.mockito.ArgumentMatchers.eq("ACTIVE"), org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(Optional.of(checkout));
        assertThat(service().pendingCheckout()).contains(new BillingCheckoutResponse("https://asaas.example/checkout-1", checkout.getExpiresAt(), "ESSENTIAL"));
    }
    private TenantBillingService service(){return new TenantBillingService(subscriptions,events,plans,checkouts,tenants,new AsaasBillingProperties(true,"unused","token","https://api.asaas.com/v3","https://app/success","https://app/cancel","https://app/expired"),new ObjectMapper(),checkoutClient);}
}
