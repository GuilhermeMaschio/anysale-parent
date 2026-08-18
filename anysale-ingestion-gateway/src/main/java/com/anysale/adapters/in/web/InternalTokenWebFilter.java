package com.anysale.adapters.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class InternalTokenWebFilter implements WebFilter {

    public static final String HEADER_NAME = "X-Internal-Token";
    private static final String PROTECTED_PATH = "/v1/messages/incoming";

    private final String configuredToken;

    public InternalTokenWebFilter(
            @Value("${internal.auth.token:${ANYSALE_INTERNAL_TOKEN:}}") String configuredToken
    ) {
        this.configuredToken = configuredToken;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!requiresInternalToken(exchange) || configuredToken == null || configuredToken.isBlank()) {
            return chain.filter(exchange);
        }

        String providedToken = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        if (configuredToken.equals(providedToken)) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] body = ("Missing or invalid " + HEADER_NAME).getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private boolean requiresInternalToken(ServerWebExchange exchange) {
        return PROTECTED_PATH.equals(exchange.getRequest().getPath().value());
    }
}
