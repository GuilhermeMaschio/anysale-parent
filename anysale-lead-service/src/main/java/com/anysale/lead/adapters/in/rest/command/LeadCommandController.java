package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.adapters.in.rest.dto.*;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.aplication.LeadService; // seu service atual
import com.anysale.lead.aplication.service.LeadAiService;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.internalauth.InternalTokenProtected;
import com.anysale.lead.idempotency.Idempotent;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/leads")
public class LeadCommandController {

    private final LeadService service;
    private final LeadAiService leadAiService;

    public LeadCommandController(LeadService service, LeadAiService leadAiService) {
        this.service = service;
        this.leadAiService = leadAiService;
    }

    /**
     * Idempotente via header Idempotency-Key (ver Interceptor/Advice).
     * - Primeira chamada: cria o Lead, retorna 201 com Location e ETag.
     * - Repetição com a MESMA key e MESMO body: retorna a MESMA resposta (short-circuit).
     * - Repetição com a MESMA key e body DIFERENTE: 409 Conflict (configurado no interceptor).
     */
    @Idempotent(operation = "LEAD_CREATE", ttlSeconds = 86_400)
    @PostMapping
    public ResponseEntity<LeadResponseDto> create(@Valid @RequestBody LeadCreateRequestDto req) {
        Lead saved = service.createLead(
                req.getName(), req.getEmail(), req.getPhone(),
                req.getSource(), req.getDesiredCategory(), req.getDesiredTags()
        );

        LeadResponseDto body = LeadMapper.toResponse(saved);

        URI self = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        String etag = (saved.getUpdatedAt() != null)
                ? ("\"" + saved.getUpdatedAt().toEpochMilli() + "\"")
                : ("\"" + saved.getId().toString() + "\"");
        return ResponseEntity.created(self)
                .eTag(etag)
                .body(body);
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<StageChangedResponseDto> changeStage(
            @PathVariable UUID id, @Valid @RequestBody StageRequestDto req) {

        StageChangedResponseDto body = service.changeStageAndReturnDto(id, req.getStage(), req.getChangedBy(), req.getReason(), req.getActualValue(), req.getLostReason());

        var self = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/v1/leads/{id}")
                .buildAndExpand(id)
                .toUri();

        String etag = "\"" + body.getUpdatedAt().toEpochMilli() + "\"";

        return ResponseEntity.ok()
                .location(self)          // aponta para o recurso completo
                .eTag(etag)              // ajuda em cache/condicionais
                .body(body);
    }

    @PatchMapping("/{id}/commercial")
    public ResponseEntity<LeadResponseDto> updateCommercial(@PathVariable UUID id, @Valid @RequestBody CommercialUpdateRequestDto body) {
        Lead saved = service.updateCommercial(id, body);
        return ResponseEntity.ok().eTag("\"" + saved.getUpdatedAt().toEpochMilli() + "\"").body(LeadMapper.toResponse(saved));
    }

    @Idempotent(operation = "LEAD_SUGGESTIONS_PATCH", resourceIdParam = "id", ttlSeconds = 86400)
    @PatchMapping("/{id}/suggestions")
    public ResponseEntity<BulkApplyResponseDto> attachSuggestions(
            @PathVariable UUID id,
            @Valid @RequestBody SuggestionPatchRequestDto body) {

        BulkApplyResponseDto out = service.attachSuggestionsBulk(id, LeadMapper.toSuggestions(body));
        return ResponseEntity.ok()
                .location(URI.create("/v1/leads/" + id + "/suggestions"))
                .eTag("\"" + out.getUpdatedAt().toEpochMilli() + "\"")
                .body(out);
    }

    @PatchMapping("/{id}/enrichment")
    @InternalTokenProtected
    public ResponseEntity<LeadResponseDto> enrich(
            @PathVariable UUID id,
            @Valid @RequestBody LeadEnrichmentRequestDto body) {

        Lead saved = service.applyEnrichment(id, body);
        LeadResponseDto response = LeadMapper.toResponse(saved);

        return ResponseEntity.ok()
                .location(URI.create("/v1/leads/" + id))
                .eTag("\"" + saved.getUpdatedAt().toEpochMilli() + "\"")
                .body(response);
    }

    @PostMapping("/{id}/ai-enrichment")
    @InternalTokenProtected
    public ResponseEntity<LeadResponseDto> regenerateAiEnrichment(@PathVariable UUID id) {
        Lead saved = leadAiService.enrichLeadFromConversation(id);
        LeadResponseDto response = LeadMapper.toResponse(saved);

        return ResponseEntity.ok()
                .location(URI.create("/v1/leads/" + id))
                .eTag("\"" + saved.getUpdatedAt().toEpochMilli() + "\"")
                .body(response);
    }

    @PostMapping("/{id}/interactions/outbound")
    @InternalTokenProtected
    public ResponseEntity<InteractionResponseDto> recordOutboundInteraction(
            @PathVariable UUID id,
            @Valid @RequestBody OutboundInteractionRequest body) {

        InteractionResponseDto response = LeadMapper.toInteractionResponse(
                service.recordOutboundInteraction(id, body)
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/interactions/status")
    @InternalTokenProtected
    public ResponseEntity<Void> updateInteractionStatus(
            @Valid @RequestBody InteractionStatusUpdateRequest body) {

        service.updateInteractionStatus(body);
        return ResponseEntity.noContent().build();
    }
}
