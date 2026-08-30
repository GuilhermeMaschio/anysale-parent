package com.anysale.lead.adapters.out.persistence;

import com.anysale.lead.domain.model.CadenceEnrollmentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CadenceEnrollmentSettingsJpaRepository extends JpaRepository<CadenceEnrollmentSettings, String> {}
