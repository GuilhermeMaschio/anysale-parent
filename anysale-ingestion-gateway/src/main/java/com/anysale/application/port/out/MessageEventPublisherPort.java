package com.anysale.application.port.out;

import com.anysale.domain.model.IncomingMessage;

public interface MessageEventPublisherPort {

    void publishIncomingMessage(IncomingMessage message);

}