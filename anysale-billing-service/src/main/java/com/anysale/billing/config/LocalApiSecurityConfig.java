package com.anysale.billing.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Keeps local development usable when Keycloak resource-server security is intentionally disabled.
 * Without this chain Spring Security falls back to HTTP Basic and blocks even the public plan catalog.
 */
@Configuration
@ConditionalOnProperty(name = "anysale.security.enabled", havingValue = "false", matchIfMissing = true)
public class LocalApiSecurityConfig {
    @Bean
    SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }
}
