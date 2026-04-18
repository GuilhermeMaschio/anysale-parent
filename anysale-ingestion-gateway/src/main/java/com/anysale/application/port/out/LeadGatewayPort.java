package com.anysale.application.port.out;

import com.anysale.domain.model.IncomingMessage;

public interface LeadGatewayPort {
    void createOrUpdateLeadFromIncomingMessage(IncomingMessage message);
}