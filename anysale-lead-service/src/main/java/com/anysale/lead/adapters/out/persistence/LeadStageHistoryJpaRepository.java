package com.anysale.lead.adapters.out.persistence;
import com.anysale.lead.domain.model.LeadStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LeadStageHistoryJpaRepository extends JpaRepository<LeadStageHistory, UUID> { List<LeadStageHistory> findByTenantIdAndLead_IdOrderByCreatedAtAsc(String tenantId, UUID leadId); }
