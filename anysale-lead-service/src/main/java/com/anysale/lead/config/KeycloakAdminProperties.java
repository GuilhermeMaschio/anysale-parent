package com.anysale.lead.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anysale.keycloak-admin")
public record KeycloakAdminProperties(
        boolean enabled,
        String issuerUri,
        String realm,
        String clientId,
        String clientSecret,
        String tenantId
) {
}
