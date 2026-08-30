package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.SalesFunnelReportDto;
import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.internalauth.InternalTokenProtected;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reports")
public class SalesReportController {
    private final LeadService service;
    public SalesReportController(LeadService service) { this.service = service; }
    @GetMapping("/sales-funnel")
    @InternalTokenProtected
    public SalesFunnelReportDto salesFunnel() { return service.salesFunnelReport(); }
}
