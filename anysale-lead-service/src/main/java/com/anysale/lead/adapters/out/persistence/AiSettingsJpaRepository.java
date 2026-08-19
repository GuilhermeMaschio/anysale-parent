package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSettingsJpaRepository extends JpaRepository<AiSettings, String> {
}
