package com.anysale.notification.adapters.out.lead;

import com.anysale.notification.adapters.out.lead.dto.LeadContactSnapshot;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LeadQueryClientTest {

    @Test
    void fetchesLeadSnapshotFromLeadService() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://lead-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LeadQueryClient client = new LeadQueryClient(builder.build());
        UUID leadId = UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127");

        server.expect(requestTo("http://lead-service/v1/internal/leads/6f4ff1a0-df0d-431e-b769-b57ec71b7127"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "6f4ff1a0-df0d-431e-b769-b57ec71b7127",
                          "phone": "5541999999999",
                          "suggestedReply": "Oi! Posso te mandar algumas opcoes."
                        }
                        """, MediaType.APPLICATION_JSON));

        LeadContactSnapshot response = client.getLead(leadId);

        assertThat(response.id()).isEqualTo(leadId);
        assertThat(response.phone()).isEqualTo("5541999999999");
        assertThat(response.suggestedReply()).isEqualTo("Oi! Posso te mandar algumas opcoes.");
        server.verify();
    }
}
