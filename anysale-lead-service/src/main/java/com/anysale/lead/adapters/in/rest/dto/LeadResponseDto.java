package com.anysale.lead.adapters.in.rest.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

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
    private String assignedTo;
    private BigDecimal estimatedValue;
    private BigDecimal actualValue;
    private String lostReason;
    private Instant closedAt;
    private String lastMessage;
    private Instant lastInteractionAt;
    private String summary;
    private String intent;
    private Integer score;
    private String nextAction;
    private String suggestedReply;
    private Instant suggestedReplyGeneratedAt;
    private String aiProviderStatus;
}
