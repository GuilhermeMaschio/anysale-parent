package com.anysale.application.port.out;

import com.anysale.domain.model.IncomingMessage;
import reactor.core.publisher.Mono;

public interface LeadGatewayPort {
    Mono<Void> createOrUpdateLeadFromIncomingMessage(IncomingMessage message);
}
