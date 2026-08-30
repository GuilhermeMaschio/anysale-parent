package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.LeadCadence;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadCadenceJpaRepository extends JpaRepository<LeadCadence, UUID> {
    @Query("select c from LeadCadence c join fetch c.lead join fetch c.playbook where c.lead.id = :leadId and c.tenantId = :tenant")
    Optional<LeadCadence> findScoped(@Param("leadId") UUID leadId, @Param("tenant") String tenant);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LeadCadence c join fetch c.lead join fetch c.playbook where c.status = 'ACTIVE' and c.nextActionAt <= :now")
    List<LeadCadence> findDueForUpdate(@Param("now") Instant now);
}
