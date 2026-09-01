package com.anysale.lead.aplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anysale.lead.adapters.out.persistence.BillingWebhookEventJpaRepository;
import com.anysale.lead.adapters.out.persistence.TenantSubscriptionJpaRepository;
import com.anysale.lead.config.AsaasBillingProperties;
import com.anysale.lead.domain.model.BillingWebhookEvent;
import com.anysale.lead.domain.model.TenantSubscription;
import com.anysale.lead.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantBillingServiceTest {
    @Mock TenantSubscriptionJpaRepository subscriptions;
    @Mock BillingWebhookEventJpaRepository events;
    @Mock TenantContext tenants;

    @Test
    void appliesConfirmedPaymentOnlyToTheMatchingSubscription() throws Exception {
        TenantSubscription subscription = new TenantSubscription();
        subscription.setStatus("PAST_DUE");
        when(events.findByProviderAndProviderEventId("ASAAS", "evt-1")).thenReturn(Optional.empty());
        when(subscriptions.findByProviderAndProviderSubscriptionId("ASAAS", "sub-1")).thenReturn(Optional.of(subscription));

        service().receiveAsaasWebhook("safe-token", new ObjectMapper().readTree("""
                {"id":"evt-1","event":"PAYMENT_RECEIVED","payment":{"subscription":"sub-1"}}
                """));

        assertThat(subscription.getStatus()).isEqualTo("ACTIVE");
        verify(subscriptions).save(subscription);
        ArgumentCaptor<BillingWebhookEvent> event = ArgumentCaptor.forClass(BillingWebhookEvent.class);
        verify(events).save(event.capture());
        assertThat(event.getValue().getProcessingResult()).isEqualTo("APPLIED");
    }

    @Test
    void ignoresAnAlreadyProcessedProviderEvent() throws Exception {
        BillingWebhookEvent existing = new BillingWebhookEvent();
        when(events.findByProviderAndProviderEventId("ASAAS", "evt-1")).thenReturn(Optional.of(existing));

        service().receiveAsaasWebhook("safe-token", new ObjectMapper().readTree("""
                {"id":"evt-1","event":"PAYMENT_RECEIVED","payment":{"subscription":"sub-1"}}
                """));

        org.mockito.Mockito.verifyNoMoreInteractions(subscriptions);
    }

    private TenantBillingService service() {
        return new TenantBillingService(subscriptions, events, tenants,
                new AsaasBillingProperties(true, "api-key-not-used-here", "safe-token"), new ObjectMapper());
    }
}
