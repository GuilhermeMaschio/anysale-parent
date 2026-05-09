package com.anysale.notification.adapters.out.lead.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class LeadServiceClientConfig {

    @Bean(name = "leadServiceRestClient")
    public RestClient leadServiceRestClient(
            RestClient.Builder builder,
            @Value("${lead-service.base-url:${LEAD_SERVICE_BASE_URL:http://localhost:8080}}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
