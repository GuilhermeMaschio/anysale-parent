package com.anysale.notification.adapters.out.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatsAppSendMessageRequest(
        @JsonProperty("messaging_product") String messagingProduct,
        @JsonProperty("recipient_type") String recipientType,
        String to,
        String type,
        Text text
) {
    private static final String MESSAGING_PRODUCT = "whatsapp";
    private static final String RECIPIENT_TYPE = "individual";
    private static final String TEXT_TYPE = "text";

    public static WhatsAppSendMessageRequest text(String to, String body) {
        return new WhatsAppSendMessageRequest(
                MESSAGING_PRODUCT,
                RECIPIENT_TYPE,
                to,
                TEXT_TYPE,
                new Text(false, body)
        );
    }

    public record Text(
            @JsonProperty("preview_url") boolean previewUrl,
            String body
    ) {
    }
}
