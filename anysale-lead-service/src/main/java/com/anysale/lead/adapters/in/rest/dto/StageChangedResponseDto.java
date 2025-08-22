package com.anysale.lead.adapters.in.rest.dto;

import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StageChangedResponseDto {
    private UUID id;
    private String oldStage;
    private String newStage;
    private Instant updatedAt;
}
