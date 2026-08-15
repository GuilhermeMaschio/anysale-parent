package com.anysale.notification.adapters.in.rest;

import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.application.WhatsAppOutboundService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WhatsAppNotificationController.class, properties = "internal.auth.token=test-token")
class WhatsAppNotificationControllerTest {
    private static final String INTERNAL_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WhatsAppOutboundService whatsAppOutboundService;

    @Test
    void sendsWhatsAppTextMessage() throws Exception {
        UUID leadId = UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127");
        when(whatsAppOutboundService.sendTextMessage(any()))
                .thenReturn(new SendWhatsAppMessageResponse(
                        leadId,
                        "5541999999999",
                        "5541999999999",
                        "wamid.outbound.001",
                        "SENT"
                ));

        mockMvc.perform(post("/v1/notifications/whatsapp/messages")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leadId": "6f4ff1a0-df0d-431e-b769-b57ec71b7127",
                                  "to": "5541999999999",
                                  "message": "Oi, posso te ajudar com a cadeira ergonomica."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadId").value("6f4ff1a0-df0d-431e-b769-b57ec71b7127"))
                .andExpect(jsonPath("$.messageId").value("wamid.outbound.001"))
                .andExpect(jsonPath("$.status").value("SENT"));

        verify(whatsAppOutboundService).sendTextMessage(any());
    }

    @Test
    void sendsSuggestedWhatsAppMessage() throws Exception {
        UUID leadId = UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127");
        when(whatsAppOutboundService.sendSuggestedMessage(any()))
                .thenReturn(new SendWhatsAppMessageResponse(
                        leadId,
                        "5541999999999",
                        "5541999999999",
                        "wamid.suggested.001",
                        "SENT"
                ));

        mockMvc.perform(post("/v1/notifications/whatsapp/messages/suggested")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leadId": "6f4ff1a0-df0d-431e-b769-b57ec71b7127"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadId").value("6f4ff1a0-df0d-431e-b769-b57ec71b7127"))
                .andExpect(jsonPath("$.messageId").value("wamid.suggested.001"))
                .andExpect(jsonPath("$.status").value("SENT"));

        verify(whatsAppOutboundService).sendSuggestedMessage(any());
    }

    @Test
    void rejectsRequestWithoutInternalToken() throws Exception {
        mockMvc.perform(post("/v1/notifications/whatsapp/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "leadId": "6f4ff1a0-df0d-431e-b769-b57ec71b7127",
                                  "to": "5541999999999",
                                  "message": "Oi, posso te ajudar com a cadeira ergonomica."
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
