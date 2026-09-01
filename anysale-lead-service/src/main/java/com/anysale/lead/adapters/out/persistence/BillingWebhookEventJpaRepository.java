package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.BillingWebhookEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingWebhookEventJpaRepository extends JpaRepository<BillingWebhookEvent, UUID> {
    Optional<BillingWebhookEvent> findByProviderAndProviderEventId(String provider, String providerEventId);
}
