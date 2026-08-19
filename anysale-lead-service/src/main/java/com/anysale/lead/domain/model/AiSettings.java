package com.anysale.lead.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ai_settings")
public class AiSettings {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;
    private boolean enabled;
    private String model;
    private int maxOutputTokens;
    private Integer monthlyRequestLimit;
    private Long monthlyTokenLimit;
    private Instant updatedAt;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public Integer getMonthlyRequestLimit() { return monthlyRequestLimit; }
    public void setMonthlyRequestLimit(Integer monthlyRequestLimit) { this.monthlyRequestLimit = monthlyRequestLimit; }
    public Long getMonthlyTokenLimit() { return monthlyTokenLimit; }
    public void setMonthlyTokenLimit(Long monthlyTokenLimit) { this.monthlyTokenLimit = monthlyTokenLimit; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
