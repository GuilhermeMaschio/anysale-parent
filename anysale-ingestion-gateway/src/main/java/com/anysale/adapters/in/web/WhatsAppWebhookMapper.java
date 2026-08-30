package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.WhatsAppWebhookPayload;
import com.anysale.application.model.MessageStatusUpdate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class WhatsAppWebhookMapper {

    private static final String CHANNEL = "WHATSAPP";
    private static final String TEXT_MESSAGE_TYPE = "text";

    public List<IncomingMessageRequest> toIncomingRequests(WhatsAppWebhookPayload payload) {
        if (payload == null) {
            return List.of();
        }

        return stream(payload.entry())
                .flatMap(entry -> stream(entry.changes()))
                .map(WhatsAppWebhookPayload.Change::value)
                .filter(Objects::nonNull)
                .flatMap(value -> toIncomingRequests(value).stream())
                .toList();
    }

    public List<MessageStatusUpdate> toStatusUpdates(WhatsAppWebhookPayload payload) {
        if (payload == null) {
            return List.of();
        }

        return stream(payload.entry())
                .flatMap(entry -> stream(entry.changes()))
                .map(WhatsAppWebhookPayload.Change::value)
                .filter(Objects::nonNull)
                .flatMap(value -> stream(value.statuses()))
                .filter(this::isSupportedStatusUpdate)
                .map(this::toStatusUpdate)
                .toList();
    }

    private List<IncomingMessageRequest> toIncomingRequests(WhatsAppWebhookPayload.Value value) {
        Map<String, String> contactNamesByWaId = contactNamesByWaId(value.contacts());

        return stream(value.messages())
                .filter(this::isSupportedTextMessage)
                .map(message -> new IncomingMessageRequest(
                        message.from(),
                        contactNamesByWaId.get(message.from()),
                        message.text().body(),
                        CHANNEL,
                        message.id()
                ))
                .toList();
    }

    private MessageStatusUpdate toStatusUpdate(WhatsAppWebhookPayload.Status status) {
        WhatsAppWebhookPayload.Error firstError = first(status.errors());

        return new MessageStatusUpdate(
                CHANNEL,
                status.id(),
                status.status(),
                parseTimestamp(status.timestamp()),
                trimToNull(status.recipientId()),
                firstError == null || firstError.code() == null ? null : String.valueOf(firstError.code()),
                firstError == null ? null : trimToNull(firstError.title()),
                firstError == null ? null : trimToNull(firstError.message())
        );
    }

    private Map<String, String> contactNamesByWaId(List<WhatsAppWebhookPayload.Contact> contacts) {
        return stream(contacts)
                .filter(contact -> StringUtils.hasText(contact.waId()))
                .filter(contact -> StringUtils.hasText(profileName(contact)))
                .collect(Collectors.toMap(
                        WhatsAppWebhookPayload.Contact::waId,
                        this::profileName,
                        (first, ignored) -> first
                ));
    }

    private String profileName(WhatsAppWebhookPayload.Contact contact) {
        if (contact.profile() == null) {
            return null;
        }
        return StringUtils.hasText(contact.profile().name()) ? contact.profile().name() : null;
    }

    private boolean isSupportedTextMessage(WhatsAppWebhookPayload.Message message) {
        return message != null
                && TEXT_MESSAGE_TYPE.equalsIgnoreCase(message.type())
                && StringUtils.hasText(message.from())
                && message.text() != null
                && StringUtils.hasText(message.text().body());
    }

    private boolean isSupportedStatusUpdate(WhatsAppWebhookPayload.Status status) {
        return status != null
                && StringUtils.hasText(status.id())
                && StringUtils.hasText(status.status());
    }

    private Instant parseTimestamp(String timestamp) {
        if (!StringUtils.hasText(timestamp)) {
            return null;
        }

        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static <T> Stream<T> stream(List<T> values) {
        return values == null ? Stream.empty() : values.stream();
    }
}
