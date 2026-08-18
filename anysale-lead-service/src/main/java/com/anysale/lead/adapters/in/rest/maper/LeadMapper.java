package com.anysale.lead.adapters.in.rest.maper;

import com.anysale.lead.adapters.in.rest.dto.InteractionResponseDto;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.in.rest.dto.LeadSuggestionDto;
import com.anysale.lead.adapters.in.rest.dto.SuggestionItemDto;
import com.anysale.lead.adapters.in.rest.dto.SuggestionPatchRequestDto;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.domain.model.LeadSuggestion;
import com.anysale.lead.domain.model.LeadStageHistory;

import java.util.ArrayList;
import java.util.List;

public final class LeadMapper {
    private LeadMapper() {}

    public static LeadResponseDto toResponse(Lead lead) {
        List<String> desiredTags = lead.getDesiredTags() == null
                ? List.of()
                : new ArrayList<>(lead.getDesiredTags());

        return LeadResponseDto.builder()
                .id(lead.getId())
                .name(lead.getName())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .source(lead.getSource())
                .desiredCategory(lead.getDesiredCategory())
                .desiredTags(desiredTags)
                .stage(lead.getStage())
                .assignedTo(lead.getAssignedTo()).estimatedValue(lead.getEstimatedValue()).actualValue(lead.getActualValue())
                .lostReason(lead.getLostReason()).closedAt(lead.getClosedAt())
                .lastMessage(lead.getLastMessage())
                .lastInteractionAt(lead.getLastInteractionAt())
                .summary(lead.getSummary())
                .intent(lead.getIntent())
                .score(lead.getScore())
                .nextAction(lead.getNextAction())
                .suggestedReply(lead.getSuggestedReply())
                .suggestedReplyGeneratedAt(lead.getSuggestedReplyGeneratedAt())
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

    public static LeadSuggestionDto toSuggestionDto(LeadSuggestion s) {
        if (s == null) return null;
        return LeadSuggestionDto.builder()
                .id(s.getId())
                .productId(s.getProductId())
                .title(s.getTitle())
                .price(s.getPrice())
                .currency(s.getCurrency())
                .vendor(s.getVendor())
                .createdAt(s.getCreatedAt())
                .build();
    }

    public static InteractionResponseDto toInteractionResponse(Interaction interaction) {
        if (interaction == null) return null;
        return InteractionResponseDto.builder()
                .id(interaction.getId())
                .message(interaction.getMessage())
                .channel(interaction.getChannel())
                .direction(interaction.getDirection())
                .externalMessageId(interaction.getExternalMessageId())
                .deliveryStatus(interaction.getDeliveryStatus())
                .deliveryStatusAt(interaction.getDeliveryStatusAt())
                .deliveryRecipientId(interaction.getDeliveryRecipientId())
                .deliveryErrorCode(interaction.getDeliveryErrorCode())
                .deliveryErrorTitle(interaction.getDeliveryErrorTitle())
                .deliveryErrorMessage(interaction.getDeliveryErrorMessage())
                .createdAt(interaction.getCreatedAt())
                .build();
    }
    public static com.anysale.lead.adapters.in.rest.dto.LeadStageHistoryResponseDto toStageHistoryResponse(LeadStageHistory item) {
        return com.anysale.lead.adapters.in.rest.dto.LeadStageHistoryResponseDto.builder().id(item.getId()).fromStage(item.getFromStage()).toStage(item.getToStage()).changedBy(item.getChangedBy()).reason(item.getReason()).createdAt(item.getCreatedAt()).build();
    }
}
