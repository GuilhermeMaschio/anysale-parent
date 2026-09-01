package com.anysale.billing.config;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(name = "anysale.security.enabled", havingValue = "true")
public class ApiSecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, @Value("${anysale.security.keycloak.client-id}") String clientId) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/billing/webhooks/asaas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/billing/plans").permitAll()
                        .requestMatchers("/v1/**").authenticated().anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter(clientId)))).build();
    }
    private Converter<Jwt, JwtAuthenticationToken> converter(String clientId) {
        return jwt -> new JwtAuthenticationToken(jwt, roles(jwt, clientId));
    }
    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> roles(Jwt jwt, String clientId) {
        Set<GrantedAuthority> result = new LinkedHashSet<>();
        add(result, (Map<String, Object>) jwt.getClaim("realm_access"));
        Map<String, Object> resources = jwt.getClaim("resource_access");
        if (resources != null && resources.get(clientId) instanceof Map<?, ?> access) add(result, (Map<String, Object>) access);
        return result;
    }
    private void add(Set<GrantedAuthority> target, Map<String, Object> access) {
        if (access == null || !(access.get("roles") instanceof Collection<?> roles)) return;
        roles.stream().filter(String.class::isInstance).map(String.class::cast).map(String::toUpperCase)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).forEach(target::add);
    }
}
