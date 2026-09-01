package com.anysale.billing.persistence;

import com.anysale.billing.domain.TenantSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, String> {
    Optional<TenantSubscription> findByProviderAndProviderSubscriptionId(String provider, String providerSubscriptionId);
}
