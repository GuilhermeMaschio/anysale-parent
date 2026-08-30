package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.aplication.LeadWhatsAppService;
import com.anysale.lead.adapters.in.rest.dto.SendLeadWhatsAppMessageResponse;
import com.anysale.lead.aplication.service.LeadAiService;
import com.anysale.lead.aplication.ai.OpenAiLeadAiAssistant;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import com.anysale.lead.config.LocalSecurityConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeadCommandController.class, properties = "internal.auth.token=test-token")
@Import(LocalSecurityConfig.class)
class LeadCommandControllerTest {
    private static final String INTERNAL_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeadService leadService;

    @MockBean
    private LeadAiService leadAiService;

    @MockBean
    private LeadWhatsAppService leadWhatsAppService;

    @MockBean
    private OpenAiLeadAiAssistant openAiLeadAiAssistant;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    void enrichReturnsUpdatedLead() throws Exception {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setName("Contato 41999999999");
        lead.setDesiredCategory("home-office");
        lead.setDesiredTags(List.of("cadeira", "ergonômica"));
        lead.setSummary("Cliente com forte intenção de compra");
        lead.setIntent("BUYING");
        lead.setScore(88);
        lead.setNextAction("Enviar proposta");
        lead.setLastMessage("Quero comprar hoje");
        lead.setLastInteractionAt(Instant.parse("2026-04-18T10:15:30Z"));
        lead.setUpdatedAt(Instant.parse("2026-04-18T10:16:30Z"));

        when(leadService.applyEnrichment(eq(leadId), any())).thenReturn(lead);

        mockMvc.perform(patch("/v1/leads/{id}/enrichment", leadId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "summary": "Cliente com forte intenção de compra",
                                  "intent": "BUYING",
                                  "desiredCategory": "home-office",
                                  "desiredTags": ["cadeira", "ergonômica"],
                                  "score": 88,
                                  "nextAction": "Enviar proposta"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Cliente com forte intenção de compra"))
                .andExpect(jsonPath("$.intent").value("BUYING"))
                .andExpect(jsonPath("$.score").value(88))
                .andExpect(jsonPath("$.nextAction").value("Enviar proposta"))
                .andExpect(jsonPath("$.lastMessage").value("Quero comprar hoje"));

        verify(leadService).applyEnrichment(eq(leadId), any());
    }

    @Test
    void recordOutboundInteractionReturnsSavedInteraction() throws Exception {
        UUID leadId = UUID.randomUUID();
        Interaction interaction = new Interaction();
        interaction.setId(UUID.fromString("a2d82b58-0c80-471b-a496-7ae8f81b9d21"));
        interaction.setMessage("Oi, posso te ajudar com a cadeira ergonomica.");
        interaction.setChannel("WHATSAPP");
        interaction.setDirection("OUT");
        interaction.setExternalMessageId("wamid.outbound.001");
        interaction.setCreatedAt(Instant.parse("2026-04-21T16:00:00Z"));

        when(leadService.recordOutboundInteraction(eq(leadId), any())).thenReturn(interaction);

        mockMvc.perform(post("/v1/leads/{id}/interactions/outbound", leadId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "Oi, posso te ajudar com a cadeira ergonomica.",
                                  "channel": "WHATSAPP",
                                  "externalMessageId": "wamid.outbound.001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("a2d82b58-0c80-471b-a496-7ae8f81b9d21"))
                .andExpect(jsonPath("$.direction").value("OUT"))
                .andExpect(jsonPath("$.externalMessageId").value("wamid.outbound.001"));

        verify(leadService).recordOutboundInteraction(eq(leadId), any());
    }

    @Test
    void sendsWhatsAppMessageUsingTheLeadDestination() throws Exception {
        UUID leadId = UUID.randomUUID();
        when(leadWhatsAppService.send(eq(leadId), any())).thenReturn(
                new SendLeadWhatsAppMessageResponse(
                        leadId, "5541999999999", "5541999999999", "wamid.outbound.001", "SENT"
                )
        );

        mockMvc.perform(post("/v1/leads/{id}/whatsapp/messages", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "message": "Oi, posso te ajudar?" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadId").value(leadId.toString()))
                .andExpect(jsonPath("$.messageId").value("wamid.outbound.001"))
                .andExpect(jsonPath("$.status").value("SENT"));

        verify(leadWhatsAppService).send(eq(leadId), any());
    }

    @Test
    void updateInteractionStatusReturnsNoContent() throws Exception {
        mockMvc.perform(post("/v1/leads/interactions/status")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "channel": "WHATSAPP",
                                  "externalMessageId": "wamid.outbound.001",
                                  "status": "delivered",
                                  "statusTimestamp": "2026-05-09T12:05:00Z",
                                  "recipientId": "5541999999999"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(leadService).updateInteractionStatus(any());
    }

    @Test
    void regenerateAiEnrichmentReturnsUpdatedLead() throws Exception {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setName("Contato 41999999999");
        lead.setSummary("Resumo de IA");
        lead.setIntent("BUYING");
        lead.setScore(90);
        lead.setNextAction("Responder no WhatsApp");
        lead.setSuggestedReply("Oi! Posso te mandar algumas opcoes.");
        lead.setUpdatedAt(Instant.parse("2026-05-24T22:00:00Z"));

        when(leadAiService.enrichLeadFromConversation(leadId)).thenReturn(lead);
        when(openAiLeadAiAssistant.lastAttemptStatus()).thenReturn("OPENAI");

        mockMvc.perform(post("/v1/leads/{id}/ai-enrichment", leadId)
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Resumo de IA"))
                .andExpect(jsonPath("$.intent").value("BUYING"))
                .andExpect(jsonPath("$.suggestedReply").value("Oi! Posso te mandar algumas opcoes."))
                .andExpect(jsonPath("$.aiProviderStatus").value("OPENAI"));

        verify(leadAiService).enrichLeadFromConversation(leadId);
    }

    @Test
    void enrichRejectsRequestWithoutInternalToken() throws Exception {
        mockMvc.perform(patch("/v1/leads/{id}/enrichment", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "intent": "BUYING"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
