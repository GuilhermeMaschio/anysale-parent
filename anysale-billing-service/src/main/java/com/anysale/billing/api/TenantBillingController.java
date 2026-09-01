package com.anysale.billing.api;

import com.anysale.billing.application.TenantBillingService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/v1/billing")
public class TenantBillingController {
    private final TenantBillingService service;
    public TenantBillingController(TenantBillingService service){this.service=service;}
    @GetMapping("/subscription") @PreAuthorize("@billingAccess.administrator(authentication)")
    public TenantSubscriptionResponse subscription(){return service.currentSubscription();}
    @GetMapping("/plans")
    public List<BillingPlanResponse> plans(){return service.availablePlans();}
    @PostMapping("/checkout") @PreAuthorize("@billingAccess.administrator(authentication)")
    public BillingCheckoutResponse checkout(@Valid @RequestBody BillingCheckoutRequest request){return service.startCheckout(request.planCode());}
    @PostMapping("/webhooks/asaas") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asaasWebhook(@RequestHeader(value="asaas-access-token",required=false) String token,@RequestBody JsonNode payload){service.receiveAsaasWebhook(token,payload);}
}
