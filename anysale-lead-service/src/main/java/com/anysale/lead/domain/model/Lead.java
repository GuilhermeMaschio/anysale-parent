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
    public List<String> getDesiredTags() { return desiredTags; }
    public void setDesiredTags(List<String> desiredTags) { this.desiredTags = desiredTags; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
