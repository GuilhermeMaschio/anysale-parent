package com.anysale.catalog.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class UserIdentityContext {
    private final boolean secured;
    public UserIdentityContext(@Value("${anysale.security.enabled:false}") boolean secured) { this.secured = secured; }
    public String userId() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt && jwt.getToken().getSubject() != null) return jwt.getToken().getSubject();
        return secured ? "authenticated-user" : "local-development-user";
    }
}
