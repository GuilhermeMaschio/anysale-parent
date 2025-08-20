package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadCreateRequestDto {
    @NotBlank private String name;
    @Email   private String email;
    private String phone;
    private String source;
    private String desiredCategory;
    @Singular("desiredTag")
    private List<String> desiredTags;
}
