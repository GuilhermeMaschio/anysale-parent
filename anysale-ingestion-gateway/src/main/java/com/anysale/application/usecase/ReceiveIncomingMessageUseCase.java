package com.anysale.application.usecase;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.IncomingMessageResponse;

public interface ReceiveIncomingMessageUseCase {
    IncomingMessageResponse execute(IncomingMessageRequest request);
}