package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.WhatsAppWebhookPayload;
import com.anysale.application.model.MessageStatusUpdate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppWebhookMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WhatsAppWebhookMapper mapper = new WhatsAppWebhookMapper();

    @Test
    void mapsMetaTextMessagePayloadToIncomingMessageRequest() throws Exception {
        WhatsAppWebhookPayload payload = objectMapper.readValue(textMessagePayload(), WhatsAppWebhookPayload.class);

        List<IncomingMessageRequest> requests = mapper.toIncomingRequests(payload);

        assertThat(requests).hasSize(1);
        IncomingMessageRequest request = requests.get(0);
        assertThat(request.phone()).isEqualTo("5541999999999");
        assertThat(request.leadName()).isEqualTo("Guilherme Maschio");
        assertThat(request.message()).isEqualTo("Quero saber mais sobre cadeira ergonomica");
        assertThat(request.channel()).isEqualTo("WHATSAPP");
        assertThat(request.externalMessageId()).isEqualTo("wamid.HBgNNTU0MTk5OTk5OTk5ORUCABIYFDk4Rjc4AA");
    }

    @Test
    void ignoresWebhookPayloadsWithoutTextMessages() throws Exception {
        WhatsAppWebhookPayload payload = objectMapper.readValue(statusPayload(), WhatsAppWebhookPayload.class);

        List<IncomingMessageRequest> requests = mapper.toIncomingRequests(payload);

        assertThat(requests).isEmpty();
    }

    @Test
    void mapsStatusPayloadToMessageStatusUpdate() throws Exception {
        WhatsAppWebhookPayload payload = objectMapper.readValue(statusPayload(), WhatsAppWebhookPayload.class);

        List<MessageStatusUpdate> updates = mapper.toStatusUpdates(payload);

        assertThat(updates).hasSize(1);
        MessageStatusUpdate update = updates.get(0);
        assertThat(update.channel()).isEqualTo("WHATSAPP");
        assertThat(update.externalMessageId()).isEqualTo("wamid.status");
        assertThat(update.status()).isEqualTo("delivered");
        assertThat(update.statusTimestamp()).isEqualTo(Instant.ofEpochSecond(1713575580L));
        assertThat(update.recipientId()).isEqualTo("5541999999999");
        assertThat(update.errorCode()).isNull();
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

    private String statusPayload() {
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
                            "statuses": [
                              {
                                "id": "wamid.status",
                                "status": "delivered",
                                "timestamp": "1713575580",
                                "recipient_id": "5541999999999"
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
