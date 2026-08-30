package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AiUsageJpaRepository extends JpaRepository<AiUsage, UUID> {
    @Query("select count(u), coalesce(sum(u.inputTokens), 0), coalesce(sum(u.outputTokens), 0), coalesce(sum(u.totalTokens), 0) from AiUsage u where u.tenantId = :tenantId and u.createdAt >= :from and u.createdAt < :until")
    List<Object[]> summarize(@Param("tenantId") String tenantId, @Param("from") Instant from, @Param("until") Instant until);
}
