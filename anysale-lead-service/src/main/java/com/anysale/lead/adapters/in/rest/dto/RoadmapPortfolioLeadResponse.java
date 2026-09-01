package com.anysale.lead.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Read model for the seller's own sales roadmap. */
public record RoadmapPortfolioLeadResponse(UUID leadId, String name, String stage,
                                           BigDecimal estimatedValue, String relationship) { }
