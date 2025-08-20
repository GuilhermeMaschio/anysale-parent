package com.anysale.lead.adapters.in.rest;

import com.anysale.lead.adapters.in.rest.dto.*;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.domain.model.Lead;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/leads")
public class LeadController {

    private final LeadService service;

    public LeadController(LeadService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LeadResponseDto> create(@Valid @RequestBody LeadCreateRequestDto req) {
        Lead saved = service.createLead(
                req.getName(),
                req.getEmail(),
                req.getPhone(),
                req.getSource(),
                req.getDesiredCategory(),
                req.getDesiredTags()
        );
        return ResponseEntity.ok(LeadMapper.toResponse(saved));
    }

    @PatchMapping("/{id}/stage")
    public ResponseEntity<LeadResponseDto> changeStage(
            @PathVariable UUID id,
            @Valid @RequestBody StageRequestDto req) {
        Lead saved = service.changeStage(id, req.getStage());
        return ResponseEntity.ok(LeadMapper.toResponse(saved));
    }

    @PatchMapping("/{id}/suggestions")
    public ResponseEntity<Void> attachSuggestions(@PathVariable UUID id,
                                                  @Valid @RequestBody SuggestionPatchRequestDto body) {
        service.attachSuggestions(id, LeadMapper.toSuggestions(body));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}")
    public LeadResponseDto get(@PathVariable UUID id) {
        return LeadMapper.toResponse(service.get(id));
    }
}
