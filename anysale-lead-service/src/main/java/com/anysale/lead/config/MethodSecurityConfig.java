package com.anysale.lead.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Protects sensitive controller methods even if a developer accidentally starts
 * the API without the Keycloak web-security profile.
 */
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
