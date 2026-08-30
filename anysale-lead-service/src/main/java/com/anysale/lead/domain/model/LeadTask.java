package com.anysale.lead.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lead_task")
public class LeadTask {
    @Id @Column(columnDefinition = "uuid") private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "lead_id", nullable = false) private Lead lead;
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 64) private String tenantId;
    @Column(nullable = false, length = 240) private String title;
    @Column(name = "task_type", nullable = false, length = 40) private String taskType;
    @Column(nullable = false, length = 20) private String priority = "NORMAL";
    @Column(nullable = false, length = 20) private String status = "OPEN";
    @Column(name = "due_at", nullable = false) private Instant dueAt;
    @Column(name = "assigned_to", length = 128) private String assignedTo;
    @Column(name = "reserved_at") private Instant reservedAt;
    @Column(name = "reservation_expires_at") private Instant reservationExpiresAt;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(length = 40) private String outcome;
    @Column(length = 1000) private String note;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void prePersist(){ if(id==null) id=UUID.randomUUID(); Instant now=Instant.now(); if(createdAt==null) createdAt=now; updatedAt=now; }
    @PreUpdate void preUpdate(){ updatedAt=Instant.now(); }
    public UUID getId(){return id;} public Lead getLead(){return lead;} public void setLead(Lead v){lead=v;} public String getTenantId(){return tenantId;} public void setTenantId(String v){tenantId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getTaskType(){return taskType;} public void setTaskType(String v){taskType=v;} public String getPriority(){return priority;} public void setPriority(String v){priority=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Instant getDueAt(){return dueAt;} public void setDueAt(Instant v){dueAt=v;} public String getAssignedTo(){return assignedTo;} public void setAssignedTo(String v){assignedTo=v;} public Instant getReservedAt(){return reservedAt;} public void setReservedAt(Instant v){reservedAt=v;} public Instant getReservationExpiresAt(){return reservationExpiresAt;} public void setReservationExpiresAt(Instant v){reservationExpiresAt=v;} public Instant getCompletedAt(){return completedAt;} public void setCompletedAt(Instant v){completedAt=v;} public String getOutcome(){return outcome;} public void setOutcome(String v){outcome=v;} public String getNote(){return note;} public void setNote(String v){note=v;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
