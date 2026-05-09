package com.anysale.notification.adapters.out.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppSendMessageResponse(
        @JsonProperty("messaging_product") String messagingProduct,
        List<Contact> contacts,
        List<Message> messages
) {
    public String firstWaId() {
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }
        return contacts.get(0).waId();
    }

    public String firstMessageId() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(0).id();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            String input,
            @JsonProperty("wa_id") String waId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String id,
            @JsonProperty("message_status") String messageStatus
    ) {
    }
}
