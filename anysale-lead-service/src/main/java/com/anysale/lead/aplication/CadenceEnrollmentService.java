package com.anysale.lead.aplication;

import com.anysale.lead.adapters.out.persistence.CadenceEnrollmentSettingsJpaRepository;
import com.anysale.lead.adapters.out.persistence.SalesPlaybookStepJpaRepository;
import com.anysale.lead.domain.model.CadenceEnrollmentSettings;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.SalesPlaybook;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadenceEnrollmentService {
    private final CadenceEnrollmentSettingsJpaRepository settings;
    private final SalesPlaybookService playbooks;
    private final SalesPlaybookStepJpaRepository steps;
    private final LeadCadenceService cadences;
    public CadenceEnrollmentService(CadenceEnrollmentSettingsJpaRepository settings, SalesPlaybookService playbooks, SalesPlaybookStepJpaRepository steps, LeadCadenceService cadences) { this.settings=settings; this.playbooks=playbooks; this.steps=steps; this.cadences=cadences; }
    @Transactional public CadenceEnrollmentSettings settings(String tenant) { return settings.findById(tenant).orElseGet(() -> { CadenceEnrollmentSettings value=new CadenceEnrollmentSettings(); value.setTenantId(tenant); value.setEnabled(false); value.setUpdatedAt(Instant.now()); return settings.save(value); }); }
    @Transactional public CadenceEnrollmentSettings update(String tenant, boolean enabled) { CadenceEnrollmentSettings value=settings(tenant); value.setEnabled(enabled); value.setUpdatedAt(Instant.now()); return settings.save(value); }
    @Transactional public void enrollIfEnabled(Lead lead) { if (!settings(lead.getTenantId()).isEnabled()) return; SalesPlaybook playbook=playbooks.resolve(lead.getId()); if (steps.findByPlaybook_IdOrderByPositionAsc(playbook.getId()).isEmpty()) return; cadences.start(lead.getId()); }
}
