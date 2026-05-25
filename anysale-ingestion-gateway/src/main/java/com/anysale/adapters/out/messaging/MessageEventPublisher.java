package com.anysale.adapters.out.messaging;

import com.anysale.application.port.out.MessageEventPublisherPort;
import com.anysale.domain.model.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageEventPublisher implements MessageEventPublisherPort {

    @Override
    public void publishIncomingMessage(IncomingMessage message) {
        log.info("Incoming message accepted for phone={} channel={}", message.getPhone(), message.getChannel());
    }
}
