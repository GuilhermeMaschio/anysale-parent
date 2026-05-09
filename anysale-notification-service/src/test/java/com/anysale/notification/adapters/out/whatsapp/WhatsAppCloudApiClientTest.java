package com.anysale.notification.adapters.out.whatsapp;

import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhatsAppCloudApiClientTest {

    @Test
    void sendsTextMessageToCloudApi() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://graph.facebook.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppCloudApiClient client = new WhatsAppCloudApiClient(
                builder.build(),
                "v20.0",
                "phone-number-id",
                "access-token"
        );

        server.expect(requestTo("https://graph.facebook.com/v20.0/phone-number-id/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "messaging_product": "whatsapp",
                          "recipient_type": "individual",
                          "to": "5541999999999",
                          "type": "text",
                          "text": {
                            "preview_url": false,
                            "body": "Oi pelo WhatsApp"
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "messaging_product": "whatsapp",
                          "contacts": [
                            {
                              "input": "5541999999999",
                              "wa_id": "5541999999999"
                            }
                          ],
                          "messages": [
                            {
                              "id": "wamid.outbound.001",
                              "message_status": "accepted"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        WhatsAppSendMessageResponse response = client.sendTextMessage("5541999999999", "Oi pelo WhatsApp");

        assertThat(response.firstWaId()).isEqualTo("5541999999999");
        assertThat(response.firstMessageId()).isEqualTo("wamid.outbound.001");
        server.verify();
    }
}
