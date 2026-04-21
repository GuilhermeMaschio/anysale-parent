package com.anysale.lead.aplication.service;

import com.anysale.lead.adapters.in.rest.dto.IncomingMessageRequest;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.in.rest.maper.LeadMapper;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.aplication.usecase.HandleIncomingMessageUseCase;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadInboundService implements HandleIncomingMessageUseCase {

    private static final String INBOUND_DIRECTION = "IN";
    private static final String CONTACTED_STAGE = "CONTACTED";
    private static final List<String> TERMINAL_STAGES = List.of("WON", "LOST");

    private final LeadJpaRepository leadRepository;
    private final InteractionJpaRepository interactionRepository;
    private final LeadEventPublisher leadEventPublisher;

    @Override
    @Transactional
    public LeadResponseDto execute(IncomingMessageRequest request) {
        String normalizedPhone = normalizePhone(request.phone());
        String normalizedChannel = normalizeChannel(request.channel());
        String externalMessageId = trimToNull(request.externalMessageId());

        if (externalMessageId != null) {
            LeadResponseDto duplicateLeadResponse = findExistingLeadResponse(normalizedChannel, externalMessageId);
            if (duplicateLeadResponse != null) {
                return duplicateLeadResponse;
            }
        }

        LeadResolution leadResolution = resolveLead(request, normalizedPhone, normalizedChannel);

        Interaction interaction = new Interaction();
        interaction.setLead(leadResolution.lead());
        interaction.setMessage(request.message().trim());
        interaction.setChannel(normalizedChannel);
        interaction.setDirection(INBOUND_DIRECTION);
        interaction.setExternalMessageId(externalMessageId);
        try {
            interactionRepository.save(interaction);
        } catch (DataIntegrityViolationException ex) {
            if (externalMessageId != null) {
                LeadResponseDto duplicateLeadResponse = findExistingLeadResponse(normalizedChannel, externalMessageId);
                if (duplicateLeadResponse != null) {
                    return duplicateLeadResponse;
                }
            }
            throw ex;
        }

        if (leadResolution.created()) {
            leadEventPublisher.publishLeadCreated(leadResolution.lead());
        }
        leadEventPublisher.publishLeadUpdated(leadResolution.lead(), "INCOMING_MESSAGE_RECEIVED");

        // TODO plug AI classification/scoring/auto-response from this flow.
        return LeadMapper.toResponse(leadResolution.lead());
    }

    private LeadResolution resolveLead(IncomingMessageRequest request, String normalizedPhone, String normalizedChannel) {
        Lead lead = leadRepository.findAllByNormalizedPhone(normalizedPhone).stream().findFirst().orElse(null);
        boolean created = false;
        String fallbackLeadName = fallbackLeadName(request.leadName(), normalizedPhone);

        if (lead == null) {
            lead = new Lead();
            lead.setPhone(normalizedPhone);
            lead.setName(fallbackLeadName);
            lead.setSource(normalizedChannel);
            created = true;
        } else {
            lead.setPhone(normalizedPhone);
            if (isBlank(lead.getName())) {
                lead.setName(fallbackLeadName);
            }
            if (isBlank(lead.getSource())) {
                lead.setSource(normalizedChannel);
            }
        }

        lead.setLastMessage(request.message().trim());
        lead.setLastInteractionAt(Instant.now());
        moveToContactedStageIfNeeded(lead);

        Lead savedLead = leadRepository.save(lead);
        return new LeadResolution(savedLead, created);
    }

    private String fallbackLeadName(String leadName, String normalizedPhone) {
        String normalizedLeadName = trimToNull(leadName);
        if (normalizedLeadName != null) {
            return normalizedLeadName;
        }
        return "Contato " + normalizedPhone;
    }

    private void moveToContactedStageIfNeeded(Lead lead) {
        if (isBlank(lead.getStage()) || "NEW".equalsIgnoreCase(lead.getStage())) {
            lead.setStage(CONTACTED_STAGE);
            return;
        }

        if (TERMINAL_STAGES.contains(lead.getStage().toUpperCase())) {
            return;
        }
    }

    private String normalizePhone(String phone) {
        String normalized = trimToNull(phone);
        if (normalized == null) {
            return null;
        }
        String digitsOnly = normalized.replaceAll("\\D", "");
        return digitsOnly.isBlank() ? normalized : digitsOnly;
    }

    private String normalizeChannel(String channel) {
        String normalized = trimToNull(channel);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private LeadResponseDto findExistingLeadResponse(String normalizedChannel, String externalMessageId) {
        return interactionRepository.findByChannelAndExternalMessageId(normalizedChannel, externalMessageId)
                .map(Interaction::getLead)
                .map(LeadMapper::toResponse)
                .orElse(null);
    }

    private record LeadResolution(Lead lead, boolean created) {
    }
}
