package com.anysale.catalog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class CatalogSecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "anysale.security.enabled", havingValue = "true")
    SecurityFilterChain secured(
            HttpSecurity http,
            @Value("${anysale.security.keycloak.client-id:anysale-console}") String clientId
    ) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v1/catalog-integrations/**").hasRole("ADMIN")
                        .requestMatchers("/v1/**").authenticated().anyRequest().denyAll())
                .oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakRoleConverter(clientId))))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "anysale.security.enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain local(HttpSecurity http) throws Exception {
        return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
    }

    private Converter<Jwt, JwtAuthenticationToken> keycloakRoleConverter(String clientId) {
        return jwt -> new JwtAuthenticationToken(jwt, extractAuthorities(jwt, clientId));
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt, String clientId) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        List<String> scopes = jwt.getClaimAsStringList("scope");
        if (scopes != null) {
            scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        }
        addRoles(authorities, (Map<String, Object>) jwt.getClaim("realm_access"));

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess.get(clientId) instanceof Map<?, ?> clientAccess) {
            addRoles(authorities, (Map<String, Object>) clientAccess);
        }
        return authorities;
    }

    private void addRoles(Set<GrantedAuthority> authorities, Map<String, Object> access) {
        if (access == null || !(access.get("roles") instanceof Collection<?> roles)) {
            return;
        }
        roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
    }
}
