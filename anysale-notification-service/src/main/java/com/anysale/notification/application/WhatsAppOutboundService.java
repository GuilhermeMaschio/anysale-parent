package com.anysale.notification.application;

import com.anysale.notification.adapters.in.messaging.LeadUpdatedListener;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.adapters.out.lead.LeadInteractionClient;
import com.anysale.notification.adapters.out.whatsapp.WhatsAppCloudApiClient;
import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageResponse;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppOutboundService {

    private static final String STATUS_SENT = "SENT";

    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final LeadInteractionClient leadInteractionClient;

    public WhatsAppOutboundService(
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            LeadInteractionClient leadInteractionClient
    ) {
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.leadInteractionClient = leadInteractionClient;
    }

    public SendWhatsAppMessageResponse sendTextMessage(SendWhatsAppMessageRequest request) {
        WhatsAppSendMessageResponse cloudApiResponse =
                whatsAppCloudApiClient.sendTextMessage(request.to(), request.message());

        String messageId = cloudApiResponse.firstMessageId();
        String waId = cloudApiResponse.firstWaId();

        if (request.leadId() != null) {
            leadInteractionClient.recordWhatsAppOutbound(request.leadId(), request.message(), messageId);
            LeadUpdatedListener.recordWhatsAppOutbound(request.leadId(), messageId, request.message());
        }

        return new SendWhatsAppMessageResponse(
                request.leadId(),
                request.to(),
                waId,
                messageId,
                STATUS_SENT
        );
    }
}
