package com.anysale.lead.aplication.usecase;

import com.anysale.lead.adapters.in.rest.dto.IncomingMessageRequest;

public interface HandleIncomingMessageUseCase {

    void execute(IncomingMessageRequest request);
}
