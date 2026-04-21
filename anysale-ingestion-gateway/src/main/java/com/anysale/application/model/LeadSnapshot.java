package com.anysale.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadSnapshot {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String source;
    private String desiredCategory;
    private List<String> desiredTags;
    private String stage;
    private String lastMessage;
    private Instant lastInteractionAt;
    private String summary;
    private String intent;
    private Integer score;
    private String nextAction;

    public UUID id() {
        return id;
    }

    public String stage() {
        return stage;
    }
}
