package com.anysale.lead.adapters.in.rest.dto;
import java.time.Instant; import java.util.UUID;
public record LeadTaskResponse(UUID id, UUID leadId, String leadName, String title, String taskType, String priority, String status, Instant dueAt, String assignedTo, Instant reservationExpiresAt, Instant completedAt, String outcome, String note, Instant createdAt) {}
