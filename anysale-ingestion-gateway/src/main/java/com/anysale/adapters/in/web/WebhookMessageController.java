package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.IncomingMessageResponse;
import com.anysale.application.usecase.ReceiveIncomingMessageUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class WebhookMessageController {

    private final ReceiveIncomingMessageUseCase receiveIncomingMessageUseCase;

    @PostMapping("/incoming")
    public Mono<ResponseEntity<IncomingMessageResponse>> receive(
            @Valid @RequestBody IncomingMessageRequest request
    ) {
        return receiveIncomingMessageUseCase.execute(request)
                .map(ResponseEntity::ok);
    }
}
