package com.anysale.lead.adapters.in.rest.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SalesFunnelReportDto {
    private long totalLeads;
    private Map<String, Long> leadsByStage;
    private long wonLeads;
    private long lostLeads;
    private BigDecimal estimatedPipelineValue;
    private BigDecimal wonRevenue;
    private BigDecimal averageTicket;
    private BigDecimal winRatePercent;
    private Map<String, Long> lossesByReason;
    private Instant generatedAt;
}
