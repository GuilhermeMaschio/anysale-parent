package com.anysale.lead.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "console_preference")
@IdClass(ConsolePreference.Id.class)
public class ConsolePreference {
    @jakarta.persistence.Id @Column(name = "tenant_id", length = 64) private String tenantId;
    @jakarta.persistence.Id @Column(name = "user_id", length = 128) private String userId;
    @Column(name = "color_theme", length = 16, nullable = false) private String colorTheme;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getColorTheme() { return colorTheme; }
    public void setColorTheme(String colorTheme) { this.colorTheme = colorTheme; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public static class Id implements Serializable {
        private String tenantId;
        private String userId;
        public Id() { }
        public Id(String tenantId, String userId) { this.tenantId = tenantId; this.userId = userId; }
        @Override public boolean equals(Object other) {
            return other instanceof Id id && Objects.equals(tenantId, id.tenantId) && Objects.equals(userId, id.userId);
        }
        @Override public int hashCode() { return Objects.hash(tenantId, userId); }
    }
}
