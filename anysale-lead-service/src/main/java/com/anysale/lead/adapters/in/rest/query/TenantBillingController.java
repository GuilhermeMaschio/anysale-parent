package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.adapters.in.rest.dto.TenantSubscriptionResponse;
import com.anysale.lead.aplication.TenantBillingService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/billing")
public class TenantBillingController {
    private final TenantBillingService service;
    public TenantBillingController(TenantBillingService service) { this.service = service; }

    @GetMapping("/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public TenantSubscriptionResponse subscription() { return service.currentSubscription(); }

    @PostMapping("/webhooks/asaas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asaasWebhook(@RequestHeader(value = "asaas-access-token", required = false) String token,
                             @RequestBody JsonNode payload) {
        service.receiveAsaasWebhook(token, payload);
    }
}
