package com.anysale.contracts.event;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento publicado quando um Lead é criado.
 * POJO compatível com Jackson (construtor no-args + getters/setters).
 */
public class LeadCreatedEvent {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String source;
    private String desiredCategory;
    private List<String> desiredTags;
    private String stage;

    public LeadCreatedEvent() {
    }

    public LeadCreatedEvent(UUID id,
                            String name,
                            String email,
                            String phone,
                            String source,
                            String desiredCategory,
                            List<String> desiredTags,
                            String stage) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.source = source;
        this.desiredCategory = desiredCategory;
        this.desiredTags = desiredTags;
        this.stage = stage;
    }

    // Getters e Setters
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

    public List<String> getDesiredTags() { return desiredTags; }
    public void setDesiredTags(List<String> desiredTags) { this.desiredTags = desiredTags; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    // equals/hashCode/toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeadCreatedEvent)) return false;
        LeadCreatedEvent that = (LeadCreatedEvent) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(email, that.email) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(source, that.source) &&
                Objects.equals(desiredCategory, that.desiredCategory) &&
                Objects.equals(desiredTags, that.desiredTags) &&
                Objects.equals(stage, that.stage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, phone, source, desiredCategory, desiredTags, stage);
    }

    @Override
    public String toString() {
        return "LeadCreatedEvent{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", source='" + source + '\'' +
                ", desiredCategory='" + desiredCategory + '\'' +
                ", desiredTags=" + desiredTags +
                ", stage='" + stage + '\'' +
                '}';
    }
}
