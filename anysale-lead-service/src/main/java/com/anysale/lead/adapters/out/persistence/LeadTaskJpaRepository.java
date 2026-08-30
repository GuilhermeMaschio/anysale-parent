package com.anysale.lead.adapters.out.persistence;
import com.anysale.lead.domain.model.LeadTask;
import java.time.Instant; import java.util.*;
import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface LeadTaskJpaRepository extends JpaRepository<LeadTask, UUID> {
 @Query("select t from LeadTask t join fetch t.lead where t.id=:id and t.tenantId=:tenant") Optional<LeadTask> findScoped(@Param("id") UUID id,@Param("tenant") String tenant);
 @Query(value="select t from LeadTask t join fetch t.lead where t.tenantId=:tenant and t.status='OPEN' order by t.dueAt asc, case t.priority when 'URGENT' then 0 when 'HIGH' then 1 when 'NORMAL' then 2 else 3 end", countQuery="select count(t) from LeadTask t where t.tenantId=:tenant and t.status='OPEN'") Page<LeadTask> findOpen(@Param("tenant") String tenant, Pageable page);
 @Query(value="select t from LeadTask t join fetch t.lead where t.tenantId=:tenant and t.assignedTo=:user and t.status='ASSIGNED' order by t.dueAt asc", countQuery="select count(t) from LeadTask t where t.tenantId=:tenant and t.assignedTo=:user and t.status='ASSIGNED'") Page<LeadTask> findMine(@Param("tenant") String tenant,@Param("user") String user,Pageable page);
 long countByTenantIdAndAssignedToAndStatus(String tenantId,String assignedTo,String status);
 @Query("update LeadTask t set t.status='OPEN', t.assignedTo=null, t.reservedAt=null, t.reservationExpiresAt=null where t.tenantId=:tenant and t.status='ASSIGNED' and t.reservationExpiresAt < :now") int releaseExpired(@Param("tenant") String tenant,@Param("now") Instant now);
 List<LeadTask> findByLead_IdOrderByCreatedAtDesc(UUID leadId);
}
