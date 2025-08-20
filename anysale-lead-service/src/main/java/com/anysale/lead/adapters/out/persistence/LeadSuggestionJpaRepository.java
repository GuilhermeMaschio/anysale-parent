package com.anysale.lead.adapters.out.persistence;


import com.anysale.lead.domain.model.LeadSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadSuggestionJpaRepository extends JpaRepository<LeadSuggestion, UUID> {
    // Ex.: List<LeadSuggestion> findByLead_Id(UUID leadId);
}
