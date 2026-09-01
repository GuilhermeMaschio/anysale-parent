package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.TenantSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSubscriptionJpaRepository extends JpaRepository<TenantSubscription, String> {
    Optional<TenantSubscription> findByProviderAndProviderSubscriptionId(String provider, String providerSubscriptionId);
}
