package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadEnrichmentRequestDto {
    @Size(max = 2000)
    private String summary;

    @Size(max = 120)
    private String intent;

    @Size(max = 80)
    private String desiredCategory;

    private List<@Size(max = 64) String> desiredTags;

    private Integer score;

    @Size(max = 500)
    private String nextAction;
}
