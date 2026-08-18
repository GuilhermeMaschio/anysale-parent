package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageResponse;
import com.anysale.application.usecase.ReceiveIncomingMessageUseCase;
import com.anysale.gateway.IngestionGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = WebhookMessageController.class, properties = "internal.auth.token=test-token")
@ContextConfiguration(classes = IngestionGatewayApplication.class)
@Import(InternalTokenWebFilter.class)
class WebhookMessageControllerTest {

    private static final String INTERNAL_TOKEN = "test-token";

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReceiveIncomingMessageUseCase receiveIncomingMessageUseCase;

    @Test
    void receivesNormalizedIncomingMessageWhenTokenMatches() {
        when(receiveIncomingMessageUseCase.execute(any()))
                .thenReturn(Mono.just(new IncomingMessageResponse(
                        "RECEIVED",
                        "5541999999999",
                        UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127"),
                        null
                )));

        webTestClient.post()
                .uri("/v1/messages/incoming")
                .header("X-Internal-Token", INTERNAL_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "phone": "+55 (41) 99999-9999",
                          "leadName": "Guilherme",
                          "message": "Quero saber mais",
                          "channel": "WHATSAPP",
                          "externalMessageId": "msg-1"
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.leadId").isEqualTo("6f4ff1a0-df0d-431e-b769-b57ec71b7127");

        verify(receiveIncomingMessageUseCase).execute(any());
    }

    @Test
    void rejectsNormalizedIncomingMessageWithoutToken() {
        webTestClient.post()
                .uri("/v1/messages/incoming")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "phone": "+55 (41) 99999-9999",
                          "leadName": "Guilherme",
                          "message": "Quero saber mais",
                          "channel": "WHATSAPP",
                          "externalMessageId": "msg-1"
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized();

        verifyNoInteractions(receiveIncomingMessageUseCase);
    }
}
