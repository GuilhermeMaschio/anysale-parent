package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InteractionJpaRepository extends JpaRepository<Interaction, UUID> {

    Optional<Interaction> findByChannelAndExternalMessageId(String channel, String externalMessageId);
}
