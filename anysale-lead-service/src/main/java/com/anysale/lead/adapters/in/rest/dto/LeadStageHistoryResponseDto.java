package com.anysale.lead.adapters.in.rest.dto;
import lombok.*; import java.time.Instant; import java.util.UUID;
@Data @Builder @NoArgsConstructor @AllArgsConstructor public class LeadStageHistoryResponseDto {
    private UUID id; private String fromStage; private String toStage; private String changedBy; private String reason; private Instant createdAt;
}
