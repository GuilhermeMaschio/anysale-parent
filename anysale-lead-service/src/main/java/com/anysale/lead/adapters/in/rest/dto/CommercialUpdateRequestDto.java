package com.anysale.lead.adapters.in.rest.dto;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import java.math.BigDecimal;
@Data @NoArgsConstructor @AllArgsConstructor
public class CommercialUpdateRequestDto {
    private String assignedTo;
    @DecimalMin(value = "0.0", inclusive = true) private BigDecimal estimatedValue;
    @DecimalMin(value = "0.0", inclusive = true) private BigDecimal actualValue;
    private String lostReason;
}
