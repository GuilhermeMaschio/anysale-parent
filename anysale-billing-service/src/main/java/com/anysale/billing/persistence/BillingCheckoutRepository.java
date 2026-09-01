package com.anysale.billing.persistence;

import com.anysale.billing.domain.BillingCheckout;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingCheckoutRepository extends JpaRepository<BillingCheckout, UUID> {
    Optional<BillingCheckout> findByProviderAndProviderCheckoutId(String provider, String providerCheckoutId);
    Optional<BillingCheckout> findByExternalReference(String externalReference);
}
