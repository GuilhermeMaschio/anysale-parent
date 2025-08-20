package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageRequestDto {
    @NotBlank
    private String stage;
}
