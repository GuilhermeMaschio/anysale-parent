package com.anysale.lead.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRecordRepository repo;
    private final ObjectMapper mapper;

    public Optional<IdempotencyRecord> find(String op, UUID resourceId, String key) {
        return repo.findByOperationAndResourceIdAndIdempotencyKey(op, resourceId, key);
    }

    public IdempotencyRecord save(String op, UUID resourceId, String key,
                                  String reqHash, ResponseEntity<?> resp,
                                  long ttlSeconds, String etag, String location, String contentType) {
        String bodyJson;
        try {
            Object body = resp.getBody();
            bodyJson = (body == null) ? null : mapper.writeValueAsString(body);
        } catch (Exception e) {
            bodyJson = null;
        }

        Instant now = Instant.now();
        IdempotencyRecord rec = IdempotencyRecord.builder()
                .operation(op).resourceId(resourceId).idempotencyKey(key)
                .requestHash(reqHash)
                .statusCode(resp.getStatusCode().value())
                .contentType(contentType)
                .etag(etag)
                .location(location)
                .responseBody(bodyJson)
                .createdAt(now)
                .expiresAt(ttlSeconds > 0 ? now.plusSeconds(ttlSeconds) : null)
                .build();
        return repo.save(rec);
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Hex(String s) {
        return sha256Hex(s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8));
    }
}
