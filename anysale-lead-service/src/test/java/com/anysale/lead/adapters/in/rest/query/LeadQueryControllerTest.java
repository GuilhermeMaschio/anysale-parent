package com.anysale.lead.adapters.in.rest.query;

import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeadQueryController.class, properties = "internal.auth.token=test-token")
class LeadQueryControllerTest {

    private static final String INTERNAL_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeadService leadService;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    void getReturnsLeadWithConversationAndEnrichmentFields() throws Exception {
        UUID leadId = UUID.randomUUID();
        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setName("Contato 41999999999");
        lead.setDesiredTags(List.of("cadeira", "ergonomica"));
        lead.setLastMessage("Quero saber mais");
        lead.setLastInteractionAt(Instant.parse("2026-04-18T10:15:30Z"));
        lead.setSummary("Cliente buscando cadeira para home office");
        lead.setIntent("BUYING");
        lead.setScore(90);
        lead.setNextAction("Enviar catalogo");

        when(leadService.get(leadId)).thenReturn(lead);

        mockMvc.perform(get("/v1/leads/{id}", leadId)
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastMessage").value("Quero saber mais"))
                .andExpect(jsonPath("$.summary").value("Cliente buscando cadeira para home office"))
                .andExpect(jsonPath("$.intent").value("BUYING"))
                .andExpect(jsonPath("$.score").value(90))
                .andExpect(jsonPath("$.nextAction").value("Enviar catalogo"));
    }

    @Test
    void interactionsReturnsConversationHistory() throws Exception {
        UUID leadId = UUID.randomUUID();
        Interaction first = new Interaction();
        first.setId(UUID.randomUUID());
        first.setMessage("Ola");
        first.setChannel("WHATSAPP");
        first.setDirection("IN");
        first.setExternalMessageId("msg-1");
        first.setCreatedAt(Instant.parse("2026-04-18T10:00:00Z"));

        Interaction second = new Interaction();
        second.setId(UUID.randomUUID());
        second.setMessage("Quero um orcamento");
        second.setChannel("WHATSAPP");
        second.setDirection("IN");
        second.setExternalMessageId("msg-2");
        second.setCreatedAt(Instant.parse("2026-04-18T10:01:00Z"));

        when(leadService.listInteractions(leadId)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/v1/leads/{id}/interactions", leadId)
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Ola"))
                .andExpect(jsonPath("$[0].channel").value("WHATSAPP"))
                .andExpect(jsonPath("$[1].message").value("Quero um orcamento"))
                .andExpect(jsonPath("$[1].externalMessageId").value("msg-2"));
    }

}
