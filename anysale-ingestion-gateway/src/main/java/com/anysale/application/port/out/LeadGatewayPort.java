package com.anysale.application.port.out;

import com.anysale.application.model.LeadSnapshot;
import com.anysale.application.model.MessageStatusUpdate;
import com.anysale.domain.model.IncomingMessage;
import reactor.core.publisher.Mono;

public interface LeadGatewayPort {
    Mono<LeadSnapshot> createOrUpdateLeadFromIncomingMessage(IncomingMessage message);

    Mono<Void> updateInteractionStatus(MessageStatusUpdate messageStatusUpdate);
}
