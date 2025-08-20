package com.anysale.contracts.event;

import java.util.Objects;
import java.util.UUID;

/**
 * Evento publicado quando um Lead é atualizado (ex.: mudança de estágio, anexar sugestões).
 */
public class LeadUpdatedEvent {

    private UUID id;
    private String stage;
    private String reason;

    public LeadUpdatedEvent() {
        // Jackson precisa do construtor sem argumentos
    }

    public LeadUpdatedEvent(UUID id, String stage, String reason) {
        this.id = id;
        this.stage = stage;
        this.reason = reason;
    }

    // Getters e Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    // equals/hashCode/toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LeadUpdatedEvent)) return false;
        LeadUpdatedEvent that = (LeadUpdatedEvent) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(stage, that.stage) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, stage, reason);
    }

    @Override
    public String toString() {
        return "LeadUpdatedEvent{" +
                "id=" + id +
                ", stage='" + stage + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
