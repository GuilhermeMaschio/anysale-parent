package com.anysale.lead.aplication;

import com.anysale.contracts.event.LeadUpdatedEvent;
import com.anysale.lead.adapters.in.rest.dto.BulkApplyResponseDto;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.in.rest.dto.LeadSuggestionDto;
import com.anysale.lead.adapters.in.rest.dto.StageChangedResponseDto;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadSuggestionJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadSuggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeadService {

    private final LeadJpaRepository leadRepo;
    private final LeadSuggestionJpaRepository suggestionRepo;
    private final LeadEventPublisher events;

    public LeadService(LeadJpaRepository leadRepo,
                       LeadSuggestionJpaRepository suggestionRepo,
                       LeadEventPublisher events) {
        this.leadRepo = leadRepo;
        this.suggestionRepo = suggestionRepo;
        this.events = events;
    }

    @Transactional
    public Lead createLead(String name, String email, String phone,
                           String source, String desiredCategory, List<String> desiredTags) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(phone);
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
        return leadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + id));
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

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    private void publishAfterCommitOrNow(Runnable r) {
        // implemente conforme seu helper atual; se já tiver, remova este stub
        r.run();
    }

}
