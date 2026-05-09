package com.anysale.notification.adapters.out.lead;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LeadInteractionClientTest {

    @Test
    void recordsOutboundInteractionInLeadService() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://lead-service");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        LeadInteractionClient client = new LeadInteractionClient(builder.build());
        UUID leadId = UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127");

        server.expect(requestTo("http://lead-service/v1/leads/6f4ff1a0-df0d-431e-b769-b57ec71b7127/interactions/outbound"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "message": "Mensagem outbound",
                          "channel": "WHATSAPP",
                          "externalMessageId": "wamid.outbound.001"
                        }
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.recordWhatsAppOutbound(leadId, "Mensagem outbound", "wamid.outbound.001");

        server.verify();
    }
}
