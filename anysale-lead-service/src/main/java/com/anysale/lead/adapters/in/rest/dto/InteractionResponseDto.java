package com.anysale.lead.adapters.in.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionResponseDto {
    private UUID id;
    private String message;
    private String channel;
    private String direction;
    private String externalMessageId;
    private Instant createdAt;
}
