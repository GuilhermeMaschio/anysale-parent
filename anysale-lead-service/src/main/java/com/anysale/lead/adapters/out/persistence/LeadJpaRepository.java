package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.Lead;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeadJpaRepository extends JpaRepository<Lead, UUID> {

    @Override
    @EntityGraph(attributePaths = "desiredTags")
    Page<Lead> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "desiredTags")
    @Query("""
         SELECT l FROM Lead l
         WHERE (:stage IS NULL OR l.stage = :stage)
           AND (:q IS NULL OR
                lower(l.name)  LIKE lower(concat('%', :q, '%')) OR
                lower(l.email) LIKE lower(concat('%', :q, '%')) OR
                lower(l.phone) LIKE lower(concat('%', :q, '%')))
         """)
    Page<Lead> search(@Param("stage") String stage,
                      @Param("q") String q,
                      Pageable pageable);


    @Query("""
         select distinct l
         from Lead l
         left join fetch l.desiredTags
         where l.id = :id
         """)
    Optional<Lead> findByIdWithTags(@Param("id") UUID id);

    @Query(value = """
         select *
         from lead
         where regexp_replace(coalesce(phone, ''), '\\D', '', 'g') = :normalizedPhone
         order by created_at asc
         """, nativeQuery = true)
    List<Lead> findAllByNormalizedPhone(@Param("normalizedPhone") String normalizedPhone);
}
