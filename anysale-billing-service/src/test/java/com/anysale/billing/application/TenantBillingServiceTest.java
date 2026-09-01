package com.anysale.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anysale.billing.config.AsaasBillingProperties;
import com.anysale.billing.domain.BillingWebhookEvent;
import com.anysale.billing.domain.TenantSubscription;
import com.anysale.billing.persistence.BillingWebhookEventRepository;
import com.anysale.billing.persistence.BillingPlanRepository;
import com.anysale.billing.persistence.TenantSubscriptionRepository;
import com.anysale.billing.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
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
    private TenantBillingService service(){return new TenantBillingService(subscriptions,events,plans,tenants,new AsaasBillingProperties(true,"unused","token"),new ObjectMapper());}
}
