package com.anysale.notification.adapters.out.whatsapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WhatsAppCloudApiConfig {

    @Bean(name = "whatsAppRestClient")
    public RestClient whatsAppRestClient(
            RestClient.Builder builder,
            @Value("${whatsapp.cloud-api.base-url:${WHATSAPP_GRAPH_API_BASE_URL:https://graph.facebook.com}}") String baseUrl
    ) {
        return builder.baseUrl(baseUrl).build();
    }
}
