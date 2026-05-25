package com.anysale.adapters.out.http.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "leadServiceWebClient")
    public WebClient leadServiceWebClient(
            WebClient.Builder builder,
            @Value("${lead-service.base-url}") String leadServiceBaseUrl
    ) {
        return builder.baseUrl(leadServiceBaseUrl).build();
    }
}
