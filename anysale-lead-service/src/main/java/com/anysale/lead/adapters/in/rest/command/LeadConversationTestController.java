package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.adapters.in.rest.dto.InteractionResponseDto;
import com.anysale.lead.adapters.in.rest.dto.TestInteractionRequest;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.aplication.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Developer aid deliberately unavailable outside the local Keycloak profile.
 */
@Profile("dev-keycloak")
@RestController
@RequestMapping("/v1/leads/{leadId}/test-interactions")
@RequiredArgsConstructor
public class LeadConversationTestController {

    private final LeadService leadService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InteractionResponseDto> add(
            @PathVariable UUID leadId,
            @Valid @RequestBody TestInteractionRequest request
    ) {
        return ResponseEntity.ok(LeadMapper.toInteractionResponse(
                leadService.recordTestInteraction(leadId, request.message(), request.direction())
        ));
    }
}
