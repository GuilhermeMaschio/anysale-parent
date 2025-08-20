package com.anysale.catalog.adapters.out.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LeadClient {
    private final WebClient web;
    public LeadClient(@Value("${lead-service.base-url}") String base){
        this.web = WebClient.builder().baseUrl(base).build();
    }
    public void patchSuggestions(String leadId, Object body){
        web.patch().uri("/v1/leads/{id}/suggestions", leadId)
                .bodyValue(body).retrieve().toBodilessEntity().block();
    }
}
