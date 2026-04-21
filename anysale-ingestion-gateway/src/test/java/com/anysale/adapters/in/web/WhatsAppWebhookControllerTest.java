package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.IncomingMessageResponse;
import com.anysale.application.usecase.ReceiveIncomingMessageUseCase;
import com.anysale.gateway.IngestionGatewayApplication;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = WhatsAppWebhookController.class)
@ContextConfiguration(classes = IngestionGatewayApplication.class)
@Import({WhatsAppWebhookMapper.class, WhatsAppWebhookSignatureVerifier.class})
@TestPropertySource(properties = {
        "whatsapp.webhook.verify-token=verify-token",
        "whatsapp.webhook.app-secret=test-secret"
})
class WhatsAppWebhookControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReceiveIncomingMessageUseCase receiveIncomingMessageUseCase;

    @Test
    void verifiesMetaWebhookChallenge() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/whatsapp/webhook")
                        .queryParam("hub.mode", "subscribe")
                        .queryParam("hub.verify_token", "verify-token")
                        .queryParam("hub.challenge", "challenge-123")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("challenge-123");
    }

    @Test
    void rejectsWebhookChallengeWithInvalidToken() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/v1/whatsapp/webhook")
                        .queryParam("hub.mode", "subscribe")
                        .queryParam("hub.verify_token", "wrong-token")
                        .queryParam("hub.challenge", "challenge-123")
                        .build())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void receivesSignedMetaTextMessagePayload() throws Exception {
        String body = textMessagePayload();
        when(receiveIncomingMessageUseCase.execute(any())).thenReturn(Mono.just(response()));

        webTestClient.post()
                .uri("/v1/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", signature(body, "test-secret"))
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        ArgumentCaptor<IncomingMessageRequest> requestCaptor = ArgumentCaptor.forClass(IncomingMessageRequest.class);
        verify(receiveIncomingMessageUseCase).execute(requestCaptor.capture());

        IncomingMessageRequest request = requestCaptor.getValue();
        assertThat(request.phone()).isEqualTo("5541999999999");
        assertThat(request.leadName()).isEqualTo("Guilherme Maschio");
        assertThat(request.message()).isEqualTo("Quero saber mais sobre cadeira ergonomica");
        assertThat(request.channel()).isEqualTo("WHATSAPP");
        assertThat(request.externalMessageId()).isEqualTo("wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA");
    }

    @Test
    void rejectsPostWhenSignatureIsInvalid() {
        webTestClient.post()
                .uri("/v1/whatsapp/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=invalid")
                .bodyValue(textMessagePayload())
                .exchange()
                .expectStatus().isForbidden();

        verifyNoInteractions(receiveIncomingMessageUseCase);
    }

    private IncomingMessageResponse response() {
        return new IncomingMessageResponse(
                "RECEIVED",
                "5541999999999",
                UUID.fromString("6f4ff1a0-df0d-431e-b769-b57ec71b7127"),
                null
        );
    }

    private String signature(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    private String textMessagePayload() {
        return """
                {
                  "object": "whatsapp_business_account",
                  "entry": [
                    {
                      "id": "123456789",
                      "changes": [
                        {
                          "field": "messages",
                          "value": {
                            "messaging_product": "whatsapp",
                            "metadata": {
                              "display_phone_number": "55 41 99999-9999",
                              "phone_number_id": "987654321"
                            },
                            "contacts": [
                              {
                                "profile": {
                                  "name": "Guilherme Maschio"
                                },
                                "wa_id": "5541999999999"
                              }
                            ],
                            "messages": [
                              {
                                "from": "5541999999999",
                                "id": "wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA",
                                "timestamp": "1713575550",
                                "text": {
                                  "body": "Quero saber mais sobre cadeira ergonomica"
                                },
                                "type": "text"
                              }
                            ]
                          }
                        }
                      ]
                    }
                  ]
                }
                """;
    }
}
