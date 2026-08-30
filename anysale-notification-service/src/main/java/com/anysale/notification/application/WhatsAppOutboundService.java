package com.anysale.notification.application;

import com.anysale.notification.adapters.in.messaging.LeadUpdatedListener;
import com.anysale.notification.adapters.in.rest.dto.SendSuggestedWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.adapters.out.lead.LeadInteractionClient;
import com.anysale.notification.adapters.out.lead.LeadQueryClient;
import com.anysale.notification.adapters.out.lead.dto.LeadContactSnapshot;
import com.anysale.notification.adapters.out.whatsapp.WhatsAppCloudApiClient;
import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WhatsAppOutboundService {

    private static final String STATUS_SENT = "SENT";

    private final WhatsAppCloudApiClient whatsAppCloudApiClient;
    private final LeadInteractionClient leadInteractionClient;
    private final LeadQueryClient leadQueryClient;

    public WhatsAppOutboundService(
            WhatsAppCloudApiClient whatsAppCloudApiClient,
            LeadInteractionClient leadInteractionClient,
            LeadQueryClient leadQueryClient
    ) {
        this.whatsAppCloudApiClient = whatsAppCloudApiClient;
        this.leadInteractionClient = leadInteractionClient;
        this.leadQueryClient = leadQueryClient;
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

    public SendWhatsAppMessageResponse sendSuggestedMessage(SendSuggestedWhatsAppMessageRequest request) {
        LeadContactSnapshot lead = leadQueryClient.getLead(request.leadId());
        String destination = firstNonBlank(request.to(), lead.phone());
        String message = trimToNull(lead.suggestedReply());

        if (destination == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lead does not have a WhatsApp destination"
            );
        }

        if (message == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lead does not have a suggested reply yet"
            );
        }

        return sendTextMessage(new SendWhatsAppMessageRequest(
                request.leadId(),
                destination,
                message
        ));
    }

    private String firstNonBlank(String preferred, String fallback) {
        String normalizedPreferred = trimToNull(preferred);
        return normalizedPreferred != null ? normalizedPreferred : trimToNull(fallback);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
