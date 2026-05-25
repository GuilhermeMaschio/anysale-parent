package com.anysale.adapters.in.web;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppWebhookSignatureVerifierTest {

    @Test
    void acceptsValidSha256Signature() throws Exception {
        WhatsAppWebhookSignatureVerifier verifier = new WhatsAppWebhookSignatureVerifier("test-secret");
        String body = "{\"object\":\"whatsapp_business_account\"}";

        assertThat(verifier.isValid(body, signature(body, "test-secret"))).isTrue();
    }

    @Test
    void rejectsInvalidSha256Signature() {
        WhatsAppWebhookSignatureVerifier verifier = new WhatsAppWebhookSignatureVerifier("test-secret");

        assertThat(verifier.isValid("{}", "sha256=invalid")).isFalse();
    }

    @Test
    void allowsUnsignedRequestsWhenAppSecretIsNotConfigured() {
        WhatsAppWebhookSignatureVerifier verifier = new WhatsAppWebhookSignatureVerifier("");

        assertThat(verifier.isValid("{}", null)).isTrue();
    }

    private String signature(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
