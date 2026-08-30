package com.anysale.lead.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UserIdentityContext {
    private final boolean secured;

    public UserIdentityContext(@Value("${anysale.security.enabled:false}") boolean secured) {
        this.secured = secured;
    }

    public String userId() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String subject = jwt.getToken().getSubject();
            if (subject != null && subject.matches("[A-Za-z0-9-]{1,128}")) return subject;
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing or invalid user identity claim");
        }
        if (!secured) return "local-development-user";
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user identity is required");
    }
}
