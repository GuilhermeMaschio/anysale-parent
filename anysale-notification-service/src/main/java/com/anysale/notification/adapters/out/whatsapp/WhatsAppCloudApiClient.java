package com.anysale.notification.adapters.out.whatsapp;

import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageRequest;
import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class WhatsAppCloudApiClient {

    private final RestClient whatsAppRestClient;
    private final String graphApiVersion;
    private final String phoneNumberId;
    private final String accessToken;

    public WhatsAppCloudApiClient(
            @Qualifier("whatsAppRestClient") RestClient whatsAppRestClient,
            @Value("${whatsapp.cloud-api.version:${WHATSAPP_GRAPH_API_VERSION:v20.0}}") String graphApiVersion,
            @Value("${whatsapp.cloud-api.phone-number-id:${WHATSAPP_PHONE_NUMBER_ID:}}") String phoneNumberId,
            @Value("${whatsapp.cloud-api.access-token:${WHATSAPP_ACCESS_TOKEN:}}") String accessToken
    ) {
        this.whatsAppRestClient = whatsAppRestClient;
        this.graphApiVersion = graphApiVersion;
        this.phoneNumberId = phoneNumberId;
        this.accessToken = accessToken;
    }

    public WhatsAppSendMessageResponse sendTextMessage(String to, String message) {
        ensureConfigured();

        WhatsAppSendMessageRequest request = WhatsAppSendMessageRequest.text(to, message);

        return whatsAppRestClient.post()
                .uri("/{version}/{phoneNumberId}/messages", graphApiVersion, phoneNumberId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(request)
                .retrieve()
                .body(WhatsAppSendMessageResponse.class);
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(phoneNumberId)) {
            throw outboundNotConfigured("WHATSAPP_PHONE_NUMBER_ID");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw outboundNotConfigured("WHATSAPP_ACCESS_TOKEN");
        }
    }

    private ResponseStatusException outboundNotConfigured(String missingSetting) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "WhatsApp outbound is not configured: " + missingSetting + " is required"
        );
    }
}
