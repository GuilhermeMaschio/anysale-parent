package com.anysale.billing.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("billingAccess")
public class BillingAccessPolicy {
    private final boolean secured;

    public BillingAccessPolicy(@Value("${anysale.security.enabled:false}") boolean secured) {
        this.secured = secured;
    }

    public boolean administrator(Authentication authentication) {
        if (!secured) return true;
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
