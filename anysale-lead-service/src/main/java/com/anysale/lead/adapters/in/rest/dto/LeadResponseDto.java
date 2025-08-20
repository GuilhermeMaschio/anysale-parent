package com.anysale.lead.adapters.in.rest.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadResponseDto {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String source;
    private String desiredCategory;
    private List<String> desiredTags;
    private String stage;
}
