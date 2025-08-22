package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.adapters.in.rest.dto.*;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.aplication.LeadService; // seu service atual
import com.anysale.lead.domain.model.Lead;
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
    public LeadCommandController(LeadService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<LeadResponseDto> create(@Valid @RequestBody LeadCreateRequestDto req) {
        Lead saved = service.createLead(
                req.getName(), req.getEmail(), req.getPhone(),
                req.getSource(), req.getDesiredCategory(), req.getDesiredTags()
        );
        return ResponseEntity.ok(LeadMapper.toResponse(saved));
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<StageChangedResponseDto> changeStage(
            @PathVariable UUID id, @Valid @RequestBody StageRequestDto req) {

        StageChangedResponseDto body = service.changeStageAndReturnDto(id, req.getStage());

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
}
