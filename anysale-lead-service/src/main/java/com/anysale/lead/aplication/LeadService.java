package com.anysale.lead.aplication;

import com.anysale.contracts.event.LeadUpdatedEvent;
import com.anysale.lead.adapters.in.rest.dto.BulkApplyResponseDto;
import com.anysale.lead.adapters.in.rest.dto.LeadEnrichmentRequestDto;
import com.anysale.lead.adapters.in.rest.dto.LeadSuggestionDto;
import com.anysale.lead.adapters.in.rest.dto.StageChangedResponseDto;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadSuggestionJpaRepository;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeadService {

    private final LeadJpaRepository leadRepo;
    private final LeadSuggestionJpaRepository suggestionRepo;
    private final InteractionJpaRepository interactionRepo;
    private final LeadEventPublisher events;

    public LeadService(LeadJpaRepository leadRepo,
                       LeadSuggestionJpaRepository suggestionRepo,
                       InteractionJpaRepository interactionRepo,
                       LeadEventPublisher events) {
        this.leadRepo = leadRepo;
        this.suggestionRepo = suggestionRepo;
        this.interactionRepo = interactionRepo;
        this.events = events;
    }

    @Transactional
    public Lead createLead(String name, String email, String phone,
                           String source, String desiredCategory, List<String> desiredTags) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(normalizePhone(phone));
        lead.setSource(source);
        lead.setDesiredCategory(desiredCategory);
        lead.setDesiredTags(desiredTags != null ? new ArrayList<>(desiredTags) : new ArrayList<>());
        Lead saved = leadRepo.saveAndFlush(lead); // flush já aqui

        // publicar APÓS o commit (ou imediatamente se não houver transação)
        publishAfterCommitOrNow(() -> events.publishLeadCreated(saved));

        return saved;
    }

    @Transactional
    public StageChangedResponseDto changeStageAndReturnDto(UUID id, String stage) {
        Lead lead = leadRepo.findByIdWithTags(id).orElseThrow();
        String old = lead.getStage();
        lead.setStage(stage);
        Lead saved = leadRepo.save(lead);

        return StageChangedResponseDto.builder()
                .id(saved.getId())
                .oldStage(old)
                .newStage(saved.getStage())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }


    @Transactional(readOnly = true)
    public Lead get(UUID id) {
        return leadRepo.findByIdWithTags(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Interaction> listInteractions(UUID leadId) {
        if (!leadRepo.existsById(leadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId);
        }
        return interactionRepo.findByLead_IdOrderByCreatedAtAsc(leadId);
    }


    @Transactional(readOnly = true)
    public Page<Lead> list(String stage, String q, int page, int size, Sort sort) {
        String stageOrNull = normalize(stage);
        String qOrNull = normalize(q);
        Pageable pageable = PageRequest.of(page, size, sort);
        return leadRepo.search(stageOrNull, qOrNull, pageable);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normalizePhone(String phone) {
        String trimmed = normalize(phone);
        if (trimmed == null) {
            return null;
        }
        String digitsOnly = trimmed.replaceAll("\\D", "");
        return digitsOnly.isBlank() ? trimmed : digitsOnly;
    }

    @Transactional
    public Lead applyEnrichment(UUID leadId, LeadEnrichmentRequestDto request) {
        Lead lead = leadRepo.findByIdWithTags(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        if (request.getSummary() != null) {
            lead.setSummary(trimToNull(request.getSummary()));
        }
        if (request.getIntent() != null) {
            lead.setIntent(trimToNull(request.getIntent()));
        }
        if (request.getDesiredCategory() != null) {
            lead.setDesiredCategory(trimToNull(request.getDesiredCategory()));
        }
        if (request.getDesiredTags() != null) {
            lead.setDesiredTags(sanitizeTags(request.getDesiredTags()));
        }
        if (request.getScore() != null) {
            lead.setScore(request.getScore());
        }
        if (request.getNextAction() != null) {
            lead.setNextAction(trimToNull(request.getNextAction()));
        }

        Lead saved = leadRepo.save(lead);
        publishAfterCommitOrNow(() -> events.publishLeadUpdated(saved, "ENRICHMENT_UPDATED"));
        return saved;
    }


    @Transactional
    public BulkApplyResponseDto attachSuggestionsBulk(UUID leadId, List<LeadSuggestion> suggestions) {
        Lead lead = leadRepo.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        List<LeadSuggestion> incoming = (suggestions == null) ? List.of() : suggestions;

        Set<String> existingProdIds = suggestionRepo.findByLead_Id(leadId).stream()
                .map(LeadSuggestion::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> batchSeen = new HashSet<>();

        List<LeadSuggestion> toPersist = new ArrayList<>();
        List<BulkApplyResponseDto.ItemResult> results = new ArrayList<>();
        int applied = 0, skipped = 0, errors = 0;

        for (LeadSuggestion s : incoming) {
            if (s == null || isBlank(s.getProductId()) || isBlank(s.getTitle())) {
                errors++;
                results.add(BulkApplyResponseDto.ItemResult.builder()
                        .status("ERROR")
                        .message("Missing productId/title")
                        .build());
                continue;
            }

            if (existingProdIds.contains(s.getProductId()) || !batchSeen.add(s.getProductId())) {
                skipped++;
                results.add(BulkApplyResponseDto.ItemResult.builder()
                        .status("SKIPPED_DUPLICATE")
                        .message("Duplicate suggestion for productId=" + s.getProductId())
                        .suggestion(LeadSuggestionDto.builder()
                                .productId(s.getProductId())
                                .title(s.getTitle())
                                .price(s.getPrice())
                                .currency(s.getCurrency())
                                .vendor(s.getVendor())
                                .build())
                        .build());
                continue;
            }

            s.setLead(lead);
            toPersist.add(s);
        }

        if (!toPersist.isEmpty()) {
            List<LeadSuggestion> saved = suggestionRepo.saveAll(toPersist);
            suggestionRepo.flush();
            for (LeadSuggestion ss : saved) {
                applied++;
                results.add(BulkApplyResponseDto.ItemResult.builder()
                        .status("APPLIED")
                        .suggestion(LeadMapper.toSuggestionDto(ss))
                        .build());
            }
        }

        // “carimba” updatedAt para refletir mudança (útil para ETag/ordenar por atualização)
        lead.setUpdatedAt(Instant.now());
        leadRepo.save(lead);

        publishAfterCommitOrNow(() ->
                events.publishLeadUpdated(new LeadUpdatedEvent(lead.getId(), lead.getStage(), "SUGGESTIONS_ATTACHED"))
        );

        return BulkApplyResponseDto.builder()
                .leadId(lead.getId())
                .applied(applied)
                .skipped(skipped)
                .errors(errors)
                .updatedAt(lead.getUpdatedAt())
                .items(results)
                .build();
    }

    private String trimToNull(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized;
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null) {
            return new ArrayList<>();
        }
        return tags.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private void publishAfterCommitOrNow(Runnable r) {
        r.run();
    }

}
