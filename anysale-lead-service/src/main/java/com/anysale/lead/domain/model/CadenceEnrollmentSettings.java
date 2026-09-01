package com.anysale.lead.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "cadence_enrollment_settings")
public class CadenceEnrollmentSettings {
    @Id @Column(name = "tenant_id") private String tenantId;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    public String getTenantId() { return tenantId; } public void setTenantId(String value) { tenantId = value; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean value) { enabled = value; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant value) { updatedAt = value; }
}
