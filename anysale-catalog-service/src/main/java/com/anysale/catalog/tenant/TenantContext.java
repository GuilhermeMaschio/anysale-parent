package com.anysale.catalog.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TenantContext {
    private final boolean secured;
    private final String localTenant;
    public TenantContext(@Value("${anysale.security.enabled:false}") boolean secured,
                         @Value("${anysale.tenant.local-id:anysale}") String localTenant) {
        this.secured = secured; this.localTenant = localTenant;
    }
    public String tenantId() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String tenant = jwt.getToken().getClaimAsString("tenant_id");
            if (tenant != null && tenant.matches("[a-z0-9][a-z0-9-]{0,62}")) return tenant;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing or invalid tenant_id claim");
        }
        if (!secured) return localTenant;
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated tenant is required");
    }
}
