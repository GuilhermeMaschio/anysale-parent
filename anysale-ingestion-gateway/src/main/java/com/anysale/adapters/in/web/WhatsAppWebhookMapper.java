package com.anysale.adapters.in.web;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.WhatsAppWebhookPayload;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

    private static <T> Stream<T> stream(List<T> values) {
        return values == null ? Stream.empty() : values.stream();
    }
}
