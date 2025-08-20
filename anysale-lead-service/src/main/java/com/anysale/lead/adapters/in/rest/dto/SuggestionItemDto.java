package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionItemDto {
    @NotBlank private String productId;
    @NotBlank private String title;
    private BigDecimal price;
    private String currency;
    private String vendor;
}
