package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.internalauth.InternalTokenProtected;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/internal/leads")
@InternalTokenProtected
public class InternalLeadQueryController {

    private final LeadService leadService;

    public InternalLeadQueryController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping("/{id}")
    public LeadContactResponse get(@PathVariable UUID id) {
        Lead lead = leadService.get(id);
        return new LeadContactResponse(lead.getId(), lead.getPhone(), lead.getSuggestedReply());
    }

    public record LeadContactResponse(UUID id, String phone, String suggestedReply) {
    }
}
