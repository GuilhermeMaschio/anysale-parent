package com.anysale.application.service;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.IncomingMessageResponse;
import com.anysale.application.port.out.LeadGatewayPort;
import com.anysale.application.port.out.MessageEventPublisherPort;
import com.anysale.application.usecase.ReceiveIncomingMessageUseCase;
import com.anysale.domain.model.IncomingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MessageIngestionService implements ReceiveIncomingMessageUseCase {

    private final LeadGatewayPort leadGatewayPort;
    private final MessageEventPublisherPort messageEventPublisherPort;

    @Override
    public Mono<IncomingMessageResponse> execute(IncomingMessageRequest request) {
        String normalizedPhone = normalizePhone(request.phone());

        IncomingMessage incomingMessage = IncomingMessage.builder()
                .phone(normalizedPhone)
                .leadName(request.leadName())
                .message(request.message())
                .channel(request.channel())
                .externalMessageId(request.externalMessageId())
                .build();

        return leadGatewayPort.createOrUpdateLeadFromIncomingMessage(incomingMessage)
                .doOnSuccess(ignored -> messageEventPublisherPort.publishIncomingMessage(incomingMessage))
                .thenReturn(new IncomingMessageResponse("RECEIVED", normalizedPhone));
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }
}
