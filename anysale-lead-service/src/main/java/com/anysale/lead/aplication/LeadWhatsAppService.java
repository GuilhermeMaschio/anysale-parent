package com.anysale.lead.aplication;

import com.anysale.lead.adapters.in.rest.dto.SendLeadWhatsAppMessageRequest;
import com.anysale.lead.adapters.in.rest.dto.SendLeadWhatsAppMessageResponse;
import com.anysale.lead.adapters.out.notification.NotificationServiceClient;
import com.anysale.lead.adapters.out.notification.dto.SendWhatsAppMessageRequest;
import com.anysale.lead.adapters.out.notification.dto.SendWhatsAppMessageResponse;
import com.anysale.lead.domain.model.Lead;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class LeadWhatsAppService {

    private final LeadService leadService;
    private final NotificationServiceClient notificationServiceClient;

    public LeadWhatsAppService(LeadService leadService, NotificationServiceClient notificationServiceClient) {
        this.leadService = leadService;
        this.notificationServiceClient = notificationServiceClient;
    }

    public SendLeadWhatsAppMessageResponse send(UUID leadId, SendLeadWhatsAppMessageRequest request) {
        Lead lead = leadService.get(leadId);
        String destination = normalizePhone(lead.getPhone());
        if (destination == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lead does not have a WhatsApp destination");
        }

        SendWhatsAppMessageResponse response = notificationServiceClient.sendTextMessage(
                new SendWhatsAppMessageRequest(leadId, destination, request.message().trim())
        );
        return new SendLeadWhatsAppMessageResponse(
                response.leadId(), response.to(), response.waId(), response.messageId(), response.status()
        );
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }
}
