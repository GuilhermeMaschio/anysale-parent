package com.anysale.lead.adapters.in.rest.command;

import com.anysale.lead.aplication.usecase.HandleIncomingMessageUseCase;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LeadInboundController.class)
class LeadInboundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HandleIncomingMessageUseCase handleIncomingMessageUseCase;

    @MockBean
    private IdempotencyService idempotencyService;

    @Test
    void returnsOkAndLeadSnapshot() throws Exception {
        when(handleIncomingMessageUseCase.execute(any()))
                .thenReturn(LeadResponseDto.builder()
                        .id(java.util.UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127"))
                        .name("Guilherme")
                        .phone("41999999999")
                        .stage("CONTACTED")
                        .lastMessage("Quero saber mais")
                        .build());

        mockMvc.perform(post("/v1/leads/incoming-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "41999999999",
                                  "leadName": "Guilherme",
                                  "message": "Quero saber mais",
                                  "channel": "WHATSAPP",
                                  "externalMessageId": "msg-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("6f4ff1a0-df0d-431e-b769-b57ec71b7127"))
                .andExpect(jsonPath("$.stage").value("CONTACTED"));

        verify(handleIncomingMessageUseCase).execute(any());
    }
}
