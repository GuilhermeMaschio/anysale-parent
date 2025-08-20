package com.anysale.lead.adapters.out.persistence;


import com.anysale.lead.domain.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadJpaRepository extends JpaRepository<Lead, UUID> {
    // Ex.: métodos de consulta específicos (opcional)
    // List<Lead> findByStage(String stage);
}
