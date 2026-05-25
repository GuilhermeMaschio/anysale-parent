package com.anysale.notification.adapters.in.rest;

import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.application.WhatsAppOutboundService;
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
    public ResponseEntity<SendWhatsAppMessageResponse> sendTextMessage(
            @Valid @RequestBody SendWhatsAppMessageRequest request
    ) {
        return ResponseEntity.ok(whatsAppOutboundService.sendTextMessage(request));
    }
}
