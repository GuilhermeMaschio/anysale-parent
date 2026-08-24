package com.anysale.lead.aplication;

import com.anysale.contracts.event.LeadUpdatedEvent;
import com.anysale.lead.adapters.in.rest.dto.BulkApplyResponseDto;
import com.anysale.lead.adapters.in.rest.dto.InteractionStatusUpdateRequest;
import com.anysale.lead.adapters.in.rest.dto.LeadEnrichmentRequestDto;
import com.anysale.lead.adapters.in.rest.dto.LeadSuggestionDto;
import com.anysale.lead.adapters.in.rest.dto.StageChangedResponseDto;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadSuggestionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadStageHistoryJpaRepository;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadSuggestion;
import com.anysale.lead.domain.model.LeadStage;
import com.anysale.lead.domain.model.LeadStageHistory;
import com.anysale.lead.tenant.TenantContext;
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
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LeadService {

    private final LeadJpaRepository leadRepo;
    private final LeadSuggestionJpaRepository suggestionRepo;
    private final InteractionJpaRepository interactionRepo;
    private final LeadEventPublisher events;
    private final LeadStageHistoryJpaRepository stageHistoryRepo;
    private final TenantContext tenantContext;

    public LeadService(LeadJpaRepository leadRepo,
                       LeadSuggestionJpaRepository suggestionRepo,
                       InteractionJpaRepository interactionRepo,
                       LeadEventPublisher events, LeadStageHistoryJpaRepository stageHistoryRepo,
                       TenantContext tenantContext) {
        this.leadRepo = leadRepo;
        this.suggestionRepo = suggestionRepo;
        this.interactionRepo = interactionRepo;
        this.events = events;
        this.stageHistoryRepo = stageHistoryRepo;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public Lead createLead(String name, String email, String phone,
                           String source, String desiredCategory, List<String> desiredTags) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(normalizePhone(phone));
        lead.setSource(source);
        lead.setTenantId(tenantContext.tenantId());
        lead.setDesiredCategory(desiredCategory);
        lead.setDesiredTags(desiredTags != null ? new ArrayList<>(desiredTags) : new ArrayList<>());
        Lead saved = leadRepo.saveAndFlush(lead); // flush já aqui
        recordStageHistory(saved, null, saved.getStage(), "SYSTEM", "LEAD_CREATED");

        // publicar APÓS o commit (ou imediatamente se não houver transação)
        publishAfterCommitOrNow(() -> events.publishLeadCreated(saved));

        return saved;
    }

    @Transactional
    public StageChangedResponseDto changeStageAndReturnDto(UUID id, String stage) {
        return changeStageAndReturnDto(id, stage, null, null, null, null);
    }

    @Transactional
    public StageChangedResponseDto changeStageAndReturnDto(UUID id, String stage, String changedBy, String reason, java.math.BigDecimal actualValue, String lostReason) {
        Lead lead = leadRepo.findByIdWithTags(id).orElseThrow();
        String old = lead.getStage();
        LeadStage current = LeadStage.from(old);
        LeadStage target = LeadStage.from(stage);
        if (!current.canMoveTo(target)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid stage transition: " + old + " -> " + target);
        if (target == LeadStage.WON && actualValue != null) lead.setActualValue(actualValue);
        if (target == LeadStage.LOST) lead.setLostReason(trimToNull(lostReason));
        if (target == LeadStage.WON || target == LeadStage.LOST) lead.setClosedAt(Instant.now());
        lead.setStage(target.name());
        Lead saved = leadRepo.save(lead);
        recordStageHistory(saved, old, target.name(), changedBy, reason);
        publishAfterCommitOrNow(() -> events.publishLeadUpdated(saved, "STAGE_CHANGED"));

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

    private void recordStageHistory(Lead lead, String from, String to, String changedBy, String reason) {
        LeadStageHistory history = new LeadStageHistory();
        history.setLead(lead); history.setTenantId(lead.getTenantId()); history.setFromStage(from); history.setToStage(to);
        history.setChangedBy(trimToNull(changedBy)); history.setReason(trimToNull(reason));
        stageHistoryRepo.save(history);
    }

    @Transactional(readOnly = true)
    public List<Interaction> listInteractions(UUID leadId) {
        if (!leadRepo.existsById(leadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId);
        }
        return interactionRepo.findByLead_IdOrderByCreatedAtAsc(leadId);
    }

    @Transactional
    public Lead updateCommercial(UUID leadId, com.anysale.lead.adapters.in.rest.dto.CommercialUpdateRequestDto request) {
        Lead lead = get(leadId);
        if (request.getAssignedTo() != null) lead.setAssignedTo(trimToNull(request.getAssignedTo()));
        if (request.getEstimatedValue() != null) lead.setEstimatedValue(request.getEstimatedValue());
        if (request.getActualValue() != null) lead.setActualValue(request.getActualValue());
        if (request.getLostReason() != null) lead.setLostReason(trimToNull(request.getLostReason()));
        Lead saved = leadRepo.save(lead);
        publishAfterCommitOrNow(() -> events.publishLeadUpdated(saved, "COMMERCIAL_DATA_UPDATED"));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LeadStageHistory> listStageHistory(UUID leadId) {
        if (!leadRepo.existsById(leadId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId);
        return stageHistoryRepo.findByLead_IdOrderByCreatedAtAsc(leadId);
    }

    @Transactional(readOnly = true)
    public com.anysale.lead.adapters.in.rest.dto.SalesFunnelReportDto salesFunnelReport() {
        List<Lead> leads = leadRepo.findAll();
        Map<String, Long> byStage = Arrays.stream(LeadStage.values()).collect(Collectors.toMap(Enum::name, ignored -> 0L, (a,b) -> a, LinkedHashMap::new));
        leads.forEach(lead -> byStage.compute(LeadStage.from(lead.getStage()).name(), (key, value) -> value + 1));
        long won = byStage.get(LeadStage.WON.name());
        long lost = byStage.get(LeadStage.LOST.name());
        BigDecimal pipeline = leads.stream().map(Lead::getEstimatedValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal revenue = leads.stream().filter(lead -> LeadStage.WON.name().equals(lead.getStage())).map(Lead::getActualValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticket = won == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(won), 2, RoundingMode.HALF_UP);
        BigDecimal winRate = leads.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(won).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(leads.size()), 2, RoundingMode.HALF_UP);
        Map<String, Long> losses = leads.stream().filter(lead -> LeadStage.LOST.name().equals(lead.getStage())).map(Lead::getLostReason).filter(Objects::nonNull).filter(value -> !value.isBlank()).collect(Collectors.groupingBy(value -> value, LinkedHashMap::new, Collectors.counting()));
        return com.anysale.lead.adapters.in.rest.dto.SalesFunnelReportDto.builder().totalLeads(leads.size()).leadsByStage(byStage).wonLeads(won).lostLeads(lost).estimatedPipelineValue(pipeline).wonRevenue(revenue).averageTicket(ticket).winRatePercent(winRate).lossesByReason(losses).generatedAt(Instant.now()).build();
    }

    @Transactional
    public Interaction recordOutboundInteraction(UUID leadId, com.anysale.lead.adapters.in.rest.dto.OutboundInteractionRequest request) {
        Lead lead = leadRepo.findByIdWithTags(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        String normalizedChannel = normalizeChannel(request.channel());
        String externalMessageId = trimToNull(request.externalMessageId());

        if (externalMessageId != null) {
            Optional<Interaction> existing = interactionRepo.findByChannelAndExternalMessageId(normalizedChannel, externalMessageId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Interaction interaction = new Interaction();
        interaction.setLead(lead);
        interaction.setTenantId(lead.getTenantId());
        interaction.setMessage(request.message().trim());
        interaction.setChannel(normalizedChannel);
        interaction.setDirection("OUT");
        interaction.setExternalMessageId(externalMessageId);

        Interaction saved = interactionRepo.save(interaction);

        lead.setLastMessage(request.message().trim());
        lead.setLastInteractionAt(Instant.now());
        leadRepo.save(lead);

        publishAfterCommitOrNow(() -> events.publishLeadUpdated(lead, "OUTBOUND_MESSAGE_SENT"));
        return saved;
    }

    /**
     * Supports local conversation fixtures only; the controller exposing it is profile-scoped.
     */
    @Transactional
    public Interaction recordTestInteraction(UUID leadId, String message, String direction) {
        Lead lead = leadRepo.findByIdWithTags(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        Interaction interaction = new Interaction();
        interaction.setLead(lead);
        interaction.setTenantId(lead.getTenantId());
        interaction.setMessage(message.trim());
        interaction.setChannel("CONSOLE_TEST");
        interaction.setDirection("OUT".equals(direction) ? "OUTBOUND" : "INBOUND");

        Interaction saved = interactionRepo.save(interaction);
        lead.setLastMessage(message.trim());
        lead.setLastInteractionAt(Instant.now());
        leadRepo.save(lead);
        return saved;
    }

    @Transactional
    public void updateInteractionStatus(InteractionStatusUpdateRequest request) {
        String normalizedChannel = normalizeChannel(request.channel());
        String externalMessageId = trimToNull(request.externalMessageId());
        String normalizedStatus = normalizeStatus(request.status());

        if (normalizedChannel == null || externalMessageId == null || normalizedStatus == null) {
            return;
        }

        Optional<Interaction> existing = interactionRepo.findByChannelAndExternalMessageId(normalizedChannel, externalMessageId);
        if (existing.isEmpty()) {
            return;
        }

        Interaction interaction = existing.get();
        Instant statusTimestamp = request.statusTimestamp() != null ? request.statusTimestamp() : Instant.now();
        Instant currentStatusTimestamp = interaction.getDeliveryStatusAt();
        if (currentStatusTimestamp != null && statusTimestamp.isBefore(currentStatusTimestamp)) {
            return;
        }

        interaction.setDeliveryStatus(normalizedStatus);
        interaction.setDeliveryStatusAt(statusTimestamp);
        interaction.setDeliveryRecipientId(trimToNull(request.recipientId()));

        if ("FAILED".equals(normalizedStatus)) {
            interaction.setDeliveryErrorCode(trimToNull(request.errorCode()));
            interaction.setDeliveryErrorTitle(trimToNull(request.errorTitle()));
            interaction.setDeliveryErrorMessage(trimToNull(request.errorMessage()));
        } else {
            interaction.setDeliveryErrorCode(null);
            interaction.setDeliveryErrorTitle(null);
            interaction.setDeliveryErrorMessage(null);
        }

        interactionRepo.save(interaction);
        publishAfterCommitOrNow(() -> events.publishLeadUpdated(interaction.getLead(), "WHATSAPP_STATUS_" + normalizedStatus));
    }


    @Transactional(readOnly = true)
    public Page<Lead> list(String stage, String q, int page, int size, Sort sort) {
        String stageOrNull = normalize(stage);
        String qOrNull = normalize(q);
        Pageable pageable = PageRequest.of(page, size, sort);
        if (stageOrNull == null && qOrNull == null) return leadRepo.findAll(pageable);
        return leadRepo.search(stageOrNull, qOrNull, pageable);
    }

    private String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String normalizeChannel(String channel) {
        String normalized = normalize(channel);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        return normalized == null ? null : normalized.toUpperCase();
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
            s.setTenantId(lead.getTenantId());
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
