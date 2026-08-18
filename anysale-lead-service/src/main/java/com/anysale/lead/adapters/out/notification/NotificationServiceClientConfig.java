package com.anysale.lead.adapters.out.notification;

import com.anysale.lead.internalauth.InternalTokenInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationServiceClientConfig {

    @Bean
    RestClient notificationServiceRestClient(
            RestClient.Builder builder,
            @Value("${notification-service.base-url}") String baseUrl,
            @Value("${internal.auth.token:}") String internalToken
    ) {
        RestClient.Builder configured = builder.baseUrl(baseUrl);
        if (internalToken != null && !internalToken.isBlank()) {
            configured.defaultHeader(InternalTokenInterceptor.HEADER_NAME, internalToken);
        }
        return configured.build();
    }
}
