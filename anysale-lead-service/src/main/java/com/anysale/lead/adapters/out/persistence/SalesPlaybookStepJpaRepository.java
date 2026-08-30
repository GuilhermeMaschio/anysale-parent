package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.SalesPlaybookStep;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesPlaybookStepJpaRepository extends JpaRepository<SalesPlaybookStep, UUID> {
    List<SalesPlaybookStep> findByPlaybook_IdOrderByPositionAsc(UUID playbookId);
    void deleteByPlaybook_Id(UUID playbookId);
}
