package com.anysale.lead.adapters.in.rest.dto;
import jakarta.validation.constraints.*; import java.time.Instant;
public record LeadTaskCreateRequest(@NotBlank @Size(max=240) String title, @NotBlank @Pattern(regexp="WHATSAPP_REPLY|CALL|SEND_PROPOSAL|SCHEDULE_MEETING|FOLLOW_UP|REACTIVATE|OTHER") String taskType, @Pattern(regexp="LOW|NORMAL|HIGH|URGENT") String priority, @NotNull @FutureOrPresent Instant dueAt, @Size(max=1000) String note) {}
