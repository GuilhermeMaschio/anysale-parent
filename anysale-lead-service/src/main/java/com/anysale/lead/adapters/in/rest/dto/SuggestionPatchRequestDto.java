package com.anysale.lead.adapters.in.rest.dto;

import lombok.*;
import jakarta.validation.Valid;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionPatchRequestDto {
    @Valid
    private List<@Valid SuggestionItemDto> suggestions;
}
