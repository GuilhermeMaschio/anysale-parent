package com.anysale.notification.adapters.out.lead;

import com.anysale.notification.adapters.out.lead.dto.OutboundInteractionRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class LeadInteractionClient {

    private final RestClient leadServiceRestClient;

    public LeadInteractionClient(@Qualifier("leadServiceRestClient") RestClient leadServiceRestClient) {
        this.leadServiceRestClient = leadServiceRestClient;
    }

    public void recordWhatsAppOutbound(UUID leadId, String message, String externalMessageId) {
        leadServiceRestClient.post()
                .uri("/v1/leads/{leadId}/interactions/outbound", leadId)
                .body(new OutboundInteractionRequest(message, "WHATSAPP", externalMessageId))
                .retrieve()
                .toBodilessEntity();
    }
}
