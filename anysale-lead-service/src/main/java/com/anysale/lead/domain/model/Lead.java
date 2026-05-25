package com.anysale.lead.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "lead")
public class Lead {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    private String name;
    private String email;
    private String phone;
    private String source;

    @Column(name = "desired_category")
    private String desiredCategory;

    private String stage;

    @Column(name = "last_message", length = 2000)
    private String lastMessage;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    @Column(length = 2000)
    private String summary;

    @Column(length = 120)
    private String intent;

    private Integer score;

    @Column(name = "next_action", length = 500)
    private String nextAction;

    // coleção de tags em tabela própria
    @ElementCollection
    @CollectionTable(name = "lead_desired_tag", joinColumns = @JoinColumn(name = "lead_id"))
    @Column(name = "tag", length = 64)
    private List<String> desiredTags = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (stage == null) stage = "NEW";
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDesiredCategory() { return desiredCategory; }
    public void setDesiredCategory(String desiredCategory) { this.desiredCategory = desiredCategory; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public Instant getLastInteractionAt() { return lastInteractionAt; }
    public void setLastInteractionAt(Instant lastInteractionAt) { this.lastInteractionAt = lastInteractionAt; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public List<String> getDesiredTags() { return desiredTags; }
    public void setDesiredTags(List<String> desiredTags) { this.desiredTags = desiredTags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
