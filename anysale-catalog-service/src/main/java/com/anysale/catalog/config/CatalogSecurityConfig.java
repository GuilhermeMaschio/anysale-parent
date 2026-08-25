package com.anysale.catalog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class CatalogSecurityConfig {
    @Bean
    @ConditionalOnProperty(name = "anysale.security.enabled", havingValue = "true")
    SecurityFilterChain secured(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v1/**").authenticated().anyRequest().denyAll())
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults())).build();
    }
    @Bean
    @ConditionalOnProperty(name = "anysale.security.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain local(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }
}
