package com.anysale.lead.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_cadence")
public class LeadCadence {
    @Id @Column(columnDefinition = "uuid") private UUID id;
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64) private String tenantId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id", nullable = false) private Lead lead;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "playbook_id", nullable = false) private SalesPlaybook playbook;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "next_position", nullable = false) private int nextPosition;
    @Column(name = "next_action_at") private Instant nextActionAt;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "paused_at") private Instant pausedAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); Instant now = Instant.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; } public void setTenantId(String value) { tenantId = value; }
    public Lead getLead() { return lead; } public void setLead(Lead value) { lead = value; }
    public SalesPlaybook getPlaybook() { return playbook; } public void setPlaybook(SalesPlaybook value) { playbook = value; }
    public String getStatus() { return status; } public void setStatus(String value) { status = value; }
    public int getNextPosition() { return nextPosition; } public void setNextPosition(int value) { nextPosition = value; }
    public Instant getNextActionAt() { return nextActionAt; } public void setNextActionAt(Instant value) { nextActionAt = value; }
    public Instant getStartedAt() { return startedAt; } public void setStartedAt(Instant value) { startedAt = value; }
    public Instant getPausedAt() { return pausedAt; } public void setPausedAt(Instant value) { pausedAt = value; }
    public Instant getCompletedAt() { return completedAt; } public void setCompletedAt(Instant value) { completedAt = value; }
}
