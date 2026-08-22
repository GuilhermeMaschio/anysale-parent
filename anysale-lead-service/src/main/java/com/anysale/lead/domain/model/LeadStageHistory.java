package com.anysale.lead.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_stage_history")
public class LeadStageHistory {
    @Id @Column(columnDefinition = "uuid") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id", nullable = false) private Lead lead;
    @Column(name = "from_stage", length = 30) private String fromStage;
    @Column(name = "to_stage", length = 30, nullable = false) private String toStage;
    @Column(name = "changed_by", length = 120) private String changedBy;
    @Column(length = 500) private String reason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }
    public void setLead(Lead lead) { this.lead = lead; } public void setFromStage(String v) { fromStage = v; }
    public void setToStage(String v) { toStage = v; } public void setChangedBy(String v) { changedBy = v; } public void setReason(String v) { reason = v; }
    public UUID getId() { return id; } public String getFromStage() { return fromStage; } public String getToStage() { return toStage; }
    public String getChangedBy() { return changedBy; } public String getReason() { return reason; } public Instant getCreatedAt() { return createdAt; }
}
