package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InteractionJpaRepository extends JpaRepository<Interaction, UUID> {

    @Deprecated default Optional<Interaction> findByChannelAndExternalMessageId(String channel, String externalMessageId) { throw new UnsupportedOperationException("tenant_id is required"); }
    @Deprecated default List<Interaction> findByLead_IdOrderByCreatedAtAsc(UUID leadId) { throw new UnsupportedOperationException("tenant_id is required"); }

    Optional<Interaction> findByTenantIdAndChannelAndExternalMessageId(String tenantId, String channel, String externalMessageId);

    List<Interaction> findByTenantIdAndLead_IdOrderByCreatedAtAsc(String tenantId, UUID leadId);
}
