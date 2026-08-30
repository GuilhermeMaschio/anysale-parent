package com.anysale.notification.adapters.in.rest;

import com.anysale.notification.adapters.in.rest.dto.SendSuggestedWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.application.WhatsAppOutboundService;
import com.anysale.notification.internalauth.InternalTokenProtected;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications/whatsapp")
public class WhatsAppNotificationController {

    private final WhatsAppOutboundService whatsAppOutboundService;

    public WhatsAppNotificationController(WhatsAppOutboundService whatsAppOutboundService) {
        this.whatsAppOutboundService = whatsAppOutboundService;
    }

    @PostMapping("/messages")
    @InternalTokenProtected
    public ResponseEntity<SendWhatsAppMessageResponse> sendTextMessage(
            @Valid @RequestBody SendWhatsAppMessageRequest request
    ) {
        return ResponseEntity.ok(whatsAppOutboundService.sendTextMessage(request));
    }

    @PostMapping("/messages/suggested")
    @InternalTokenProtected
    public ResponseEntity<SendWhatsAppMessageResponse> sendSuggestedMessage(
            @Valid @RequestBody SendSuggestedWhatsAppMessageRequest request
    ) {
        return ResponseEntity.ok(whatsAppOutboundService.sendSuggestedMessage(request));
    }
}
