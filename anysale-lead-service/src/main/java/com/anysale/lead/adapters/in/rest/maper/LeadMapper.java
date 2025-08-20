package com.anysale.lead.adapters.in.rest.maper;

import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.in.rest.dto.SuggestionItemDto;
import com.anysale.lead.adapters.in.rest.dto.SuggestionPatchRequestDto;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadSuggestion;

import java.util.ArrayList;
import java.util.List;

public final class LeadMapper {
    private LeadMapper() {}

    public static LeadResponseDto toResponse(Lead lead) {
        return LeadResponseDto.builder()
                .id(lead.getId())
                .name(lead.getName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .source(lead.getSource())
                .desiredCategory(lead.getDesiredCategory())
                .desiredTags(lead.getDesiredTags())
                .stage(lead.getStage())
                .build();
    }

    public static List<LeadSuggestion> toSuggestions(SuggestionPatchRequestDto req) {
        List<LeadSuggestion> out = new ArrayList<>();
        if (req.getSuggestions() == null) return out;
        for (SuggestionItemDto it : req.getSuggestions()) {
            LeadSuggestion s = new LeadSuggestion();
            s.setProductId(it.getProductId());
            s.setTitle(it.getTitle());
            s.setPrice(it.getPrice());
            s.setCurrency(it.getCurrency());
            s.setVendor(it.getVendor());
            out.add(s);
        }
        return out;
    }
}
