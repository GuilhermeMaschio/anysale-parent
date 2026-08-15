package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.adapters.in.rest.dto.IncomingMessageRequest;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.aplication.usecase.HandleIncomingMessageUseCase;
import com.anysale.lead.internalauth.InternalTokenProtected;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/leads")
@RequiredArgsConstructor
public class LeadInboundController {

    private final HandleIncomingMessageUseCase handleIncomingMessageUseCase;

    @PostMapping("/incoming-message")
    @InternalTokenProtected
    public ResponseEntity<LeadResponseDto> handle(@Valid @RequestBody IncomingMessageRequest request) {
        LeadResponseDto response = handleIncomingMessageUseCase.execute(request);
        return ResponseEntity.ok(response);
    }
}
