package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.AiSkillOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiSkillOverrideJpaRepository extends JpaRepository<AiSkillOverride, UUID> {
    Optional<AiSkillOverride> findByTenantIdAndProfile(String tenantId, String profile);
    List<AiSkillOverride> findByTenantId(String tenantId);
}
