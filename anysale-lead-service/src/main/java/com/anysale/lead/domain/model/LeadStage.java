package com.anysale.lead.domain.model;

import java.util.EnumSet;

public enum LeadStage {
    NEW, CONTACTED, QUALIFIED, PROPOSAL, WON, LOST;

    public static LeadStage from(String value) {
        try { return LeadStage.valueOf(value.trim().toUpperCase()); }
        catch (Exception ex) { throw new IllegalArgumentException("Invalid lead stage: " + value); }
    }

    public boolean canMoveTo(LeadStage target) {
        if (this == target) return true;
        return switch (this) {
            case NEW -> EnumSet.of(CONTACTED, LOST).contains(target);
            case CONTACTED -> EnumSet.of(QUALIFIED, LOST).contains(target);
            case QUALIFIED -> EnumSet.of(PROPOSAL, WON, LOST).contains(target);
            case PROPOSAL -> EnumSet.of(QUALIFIED, WON, LOST).contains(target);
            case WON, LOST -> false;
        };
    }
}
