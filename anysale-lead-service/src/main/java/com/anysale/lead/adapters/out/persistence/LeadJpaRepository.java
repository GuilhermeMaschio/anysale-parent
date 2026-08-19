package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.Lead;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadJpaRepository extends JpaRepository<Lead, UUID> {

    /** Legacy test helper; application code must use the tenant-scoped method. */
    @Deprecated default Optional<Lead> findByIdWithTags(UUID id) { throw new UnsupportedOperationException("tenant_id is required"); }
    @Deprecated default List<Lead> findAllByNormalizedPhone(String normalizedPhone) { throw new UnsupportedOperationException("tenant_id is required"); }

    @EntityGraph(attributePaths = "desiredTags")
    Page<Lead> findByTenantId(String tenantId, Pageable pageable);

    @EntityGraph(attributePaths = "desiredTags")
    @Query("""
         SELECT l FROM Lead l
         WHERE l.tenantId = :tenantId AND (:stage IS NULL OR l.stage = :stage)
           AND (:q IS NULL OR
                lower(l.name)  LIKE lower(concat('%', :q, '%')) OR
                lower(l.email) LIKE lower(concat('%', :q, '%')) OR
                lower(l.phone) LIKE lower(concat('%', :q, '%')))
         """)
    Page<Lead> search(@Param("tenantId") String tenantId, @Param("stage") String stage,
                      @Param("q") String q,
                      Pageable pageable);


    @Query("""
         select distinct l
         from Lead l
         left join fetch l.desiredTags
         where l.id = :id and l.tenantId = :tenantId
         """)
    Optional<Lead> findByIdWithTags(@Param("id") UUID id, @Param("tenantId") String tenantId);

    @Query(value = """
         select *
         from lead
         where tenant_id = :tenantId and regexp_replace(coalesce(phone, ''), '\\D', '', 'g') = :normalizedPhone
         order by created_at asc
         """, nativeQuery = true)
    List<Lead> findAllByNormalizedPhone(@Param("tenantId") String tenantId, @Param("normalizedPhone") String normalizedPhone);
}
