package com.anysale.adapters.out.http.config;

import com.anysale.adapters.in.web.InternalTokenWebFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "leadServiceWebClient")
    public WebClient leadServiceWebClient(
            WebClient.Builder builder,
            @Value("${lead-service.base-url}") String leadServiceBaseUrl,
            @Value("${internal.auth.token:${ANYSALE_INTERNAL_TOKEN:}}") String internalToken
    ) {
        WebClient.Builder configuredBuilder = builder.baseUrl(leadServiceBaseUrl);
        if (internalToken != null && !internalToken.isBlank()) {
            configuredBuilder.defaultHeader(InternalTokenWebFilter.HEADER_NAME, internalToken);
        }
        return configuredBuilder.build();
    }
}
