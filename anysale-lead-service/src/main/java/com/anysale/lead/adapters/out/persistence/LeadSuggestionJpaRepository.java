package com.anysale.lead.adapters.out.persistence;


import com.anysale.lead.domain.model.LeadSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeadSuggestionJpaRepository extends JpaRepository<LeadSuggestion, UUID> {

    List<LeadSuggestion> findByLead_Id(java.util.UUID leadId);
}
