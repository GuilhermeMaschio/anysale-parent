package com.anysale.lead.adapters.out.notification;

import com.anysale.lead.adapters.out.notification.dto.SendWhatsAppMessageRequest;
import com.anysale.lead.adapters.out.notification.dto.SendWhatsAppMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class NotificationServiceClient {

    private final RestClient notificationServiceRestClient;

    public NotificationServiceClient(RestClient notificationServiceRestClient) {
        this.notificationServiceRestClient = notificationServiceRestClient;
    }

    public SendWhatsAppMessageResponse sendTextMessage(SendWhatsAppMessageRequest request) {
        try {
            return notificationServiceRestClient.post()
                    .uri("/v1/notifications/whatsapp/messages")
                    .body(request)
                    .retrieve()
                    .body(SendWhatsAppMessageResponse.class);
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(exception.getStatusCode().value()),
                    "WhatsApp message could not be sent"
            );
        }
    }
}
