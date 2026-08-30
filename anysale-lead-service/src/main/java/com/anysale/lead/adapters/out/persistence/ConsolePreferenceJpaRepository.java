package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.ConsolePreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsolePreferenceJpaRepository extends JpaRepository<ConsolePreference, ConsolePreference.Id> { }
