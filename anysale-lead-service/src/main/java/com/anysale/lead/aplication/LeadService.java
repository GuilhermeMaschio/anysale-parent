package com.anysale.lead.aplication;

import com.anysale.contracts.event.LeadUpdatedEvent;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadSuggestionJpaRepository;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadSuggestion;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    public Lead changeStage(UUID id, String newStage) {
        Lead lead = leadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + id));

        lead.setStage(newStage);
        Lead saved = leadRepo.saveAndFlush(lead);

        publishAfterCommitOrNow(() ->
                events.publishLeadUpdated(new LeadUpdatedEvent(saved.getId(), saved.getStage(), "STAGE_CHANGED"))
        );

        return saved;
    }

    @Transactional
    public void attachSuggestions(UUID leadId, List<LeadSuggestion> suggestions) {
        Lead lead = leadRepo.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + leadId));

        if (suggestions != null) {
            for (LeadSuggestion s : suggestions) {
                s.setLead(lead);
                suggestionRepo.save(s);
            }
            suggestionRepo.flush();
        }

        publishAfterCommitOrNow(() ->
                events.publishLeadUpdated(new LeadUpdatedEvent(lead.getId(), lead.getStage(), "SUGGESTIONS_ATTACHED"))
        );
    }

    @Transactional(readOnly = true)
    public Lead get(UUID id) {
        return leadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead not found: " + id));
    }

    // --------- helpers ---------

    private void publishAfterCommitOrNow(Runnable publisher) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { publisher.run(); }
                    catch (Exception e) { System.err.println("WARN publish failed: " + e.getMessage()); }
                }
            });
        } else {
            // fallback: sem transação ativa (ex.: chamada fora do contexto HTTP)
            try { publisher.run(); }
            catch (Exception e) { System.err.println("WARN publish (no TX) failed: " + e.getMessage()); }
        }
    }
}
