package com.anysale.billing.persistence;

import com.anysale.billing.domain.BillingWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEvent, UUID> {
    Optional<BillingWebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
