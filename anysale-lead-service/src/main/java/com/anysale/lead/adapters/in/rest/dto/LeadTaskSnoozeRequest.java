package com.anysale.lead.adapters.in.rest.dto;
import jakarta.validation.constraints.*; import java.time.Instant;
public record LeadTaskSnoozeRequest(@NotNull @Future Instant dueAt, @Size(max=1000) String note) {}
