package com.anysale.application.usecase;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.IncomingMessageResponse;
import reactor.core.publisher.Mono;

public interface ReceiveIncomingMessageUseCase {
    Mono<IncomingMessageResponse> execute(IncomingMessageRequest request);
}
