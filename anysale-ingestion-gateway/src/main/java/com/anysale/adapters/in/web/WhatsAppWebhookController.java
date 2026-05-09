package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.WhatsAppWebhookPayload;
import com.anysale.application.model.MessageStatusUpdate;
import com.anysale.application.port.out.LeadGatewayPort;
import com.anysale.application.usecase.ReceiveIncomingMessageUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/v1/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final ReceiveIncomingMessageUseCase receiveIncomingMessageUseCase;
    private final LeadGatewayPort leadGatewayPort;
    private final WhatsAppWebhookMapper whatsAppWebhookMapper;
    private final WhatsAppWebhookSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final String verifyToken;

    public WhatsAppWebhookController(
            ReceiveIncomingMessageUseCase receiveIncomingMessageUseCase,
            LeadGatewayPort leadGatewayPort,
            WhatsAppWebhookMapper whatsAppWebhookMapper,
            WhatsAppWebhookSignatureVerifier signatureVerifier,
            ObjectMapper objectMapper,
            @Value("${whatsapp.webhook.verify-token:${WHATSAPP_WEBHOOK_VERIFY_TOKEN:}}") String verifyToken
    ) {
        this.receiveIncomingMessageUseCase = receiveIncomingMessageUseCase;
        this.leadGatewayPort = leadGatewayPort;
        this.whatsAppWebhookMapper = whatsAppWebhookMapper;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.verifyToken = verifyToken == null ? "" : verifyToken;
    }

    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token) && StringUtils.hasText(challenge)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public Mono<ResponseEntity<Void>> receive(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody(required = false) Mono<String> rawBody
    ) {
        if (rawBody == null) {
            return handleWebhookBody("", signature);
        }
        return rawBody.defaultIfEmpty("")
                .flatMap(body -> handleWebhookBody(body, signature));
    }

    private Mono<ResponseEntity<Void>> handleWebhookBody(String rawBody, String signature) {
        if (!signatureVerifier.isValid(rawBody, signature)) {
            return Mono.just(emptyResponse(HttpStatus.FORBIDDEN));
        }

        WhatsAppWebhookPayload payload;
        try {
            payload = objectMapper.readValue(rawBody, WhatsAppWebhookPayload.class);
        } catch (JsonProcessingException ex) {
            return Mono.just(emptyResponse(HttpStatus.BAD_REQUEST));
        }

        List<IncomingMessageRequest> incomingMessages = whatsAppWebhookMapper.toIncomingRequests(payload);
        List<MessageStatusUpdate> statusUpdates = whatsAppWebhookMapper.toStatusUpdates(payload);

        if (incomingMessages.isEmpty() && statusUpdates.isEmpty()) {
            return Mono.just(emptyResponse(HttpStatus.OK));
        }

        Mono<Void> processIncomingMessages = Flux.fromIterable(incomingMessages)
                .flatMap(receiveIncomingMessageUseCase::execute)
                .then();

        Mono<Void> processStatusUpdates = Flux.fromIterable(statusUpdates)
                .flatMap(leadGatewayPort::updateInteractionStatus)
                .then();

        return Mono.when(processIncomingMessages, processStatusUpdates)
                .then(Mono.just(emptyResponse(HttpStatus.OK)));
    }

    private static ResponseEntity<Void> emptyResponse(HttpStatus status) {
        return ResponseEntity.status(status).build();
    }
}
