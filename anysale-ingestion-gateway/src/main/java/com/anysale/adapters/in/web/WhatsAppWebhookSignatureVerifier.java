package com.anysale.adapters.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WhatsAppWebhookSignatureVerifier {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final String appSecret;

    public WhatsAppWebhookSignatureVerifier(
            @Value("${whatsapp.webhook.app-secret:${WHATSAPP_APP_SECRET:}}") String appSecret
    ) {
        this.appSecret = appSecret;
    }

    public boolean isValid(String rawBody, String signature) {
        if (!StringUtils.hasText(appSecret)) {
            return true;
        }

        if (!StringUtils.hasText(signature) || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        String expectedSignature = SIGNATURE_PREFIX + hmacSha256(rawBody == null ? "" : rawBody);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String hmacSha256(String rawBody) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            return HexFormat.of().formatHex(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not verify WhatsApp webhook signature", ex);
        }
    }
}
