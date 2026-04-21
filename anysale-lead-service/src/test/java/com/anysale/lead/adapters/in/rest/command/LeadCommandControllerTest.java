package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.aplication.LeadService;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeadCommandController.class)
class LeadCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LeadService leadService;

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
}
