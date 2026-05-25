package com.anysale.lead.aplication.usecase;

import com.anysale.lead.adapters.in.rest.dto.IncomingMessageRequest;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;

public interface HandleIncomingMessageUseCase {

    LeadResponseDto execute(IncomingMessageRequest request);
}
