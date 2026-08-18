package com.anysale.notification.adapters.out.lead;

import com.anysale.notification.adapters.out.lead.dto.LeadContactSnapshot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class LeadQueryClient {

    private final RestClient leadServiceRestClient;

    public LeadQueryClient(@Qualifier("leadServiceRestClient") RestClient leadServiceRestClient) {
        this.leadServiceRestClient = leadServiceRestClient;
    }

    public LeadContactSnapshot getLead(UUID leadId) {
        return leadServiceRestClient.get()
                .uri("/v1/internal/leads/{leadId}", leadId)
                .retrieve()
                .body(LeadContactSnapshot.class);
    }
}
