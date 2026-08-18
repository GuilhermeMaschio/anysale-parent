package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageRequestDto {
    @NotBlank
    private String stage;
    private String changedBy;
    private String reason;
    private BigDecimal actualValue;
    private String lostReason;
}
