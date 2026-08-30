package com.anysale.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WhatsAppWebhookPayload(
        String object,
        List<Entry> entry
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            String id,
            List<Change> changes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(
            String field,
            Value value
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            Metadata metadata,
            List<Contact> contacts,
            List<Message> messages,
            List<Status> statuses
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(
            @JsonProperty("display_phone_number") String displayPhoneNumber,
            @JsonProperty("phone_number_id") String phoneNumberId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(
            Profile profile,
            @JsonProperty("wa_id") String waId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String from,
            String id,
            String timestamp,
            String type,
            Text text
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(
            String body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            String id,
            @JsonProperty("recipient_id") String recipientId,
            String status,
            String timestamp,
            List<Error> errors
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(
            Integer code,
            String title,
            String message
    ) {
    }
}
