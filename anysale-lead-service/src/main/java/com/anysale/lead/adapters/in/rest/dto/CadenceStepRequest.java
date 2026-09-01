package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadenceStepRequest(
        @Min(0) @Max(525600) int delayMinutes,
        @NotBlank @Size(max = 240) String title,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "WHATSAPP_REPLY|CALL|SEND_PROPOSAL|SCHEDULE_MEETING|FOLLOW_UP|REACTIVATE|OTHER") String taskType,
        @jakarta.validation.constraints.Pattern(regexp = "LOW|NORMAL|HIGH|URGENT") String priority,
        @Size(max = 1000) String note) { }
