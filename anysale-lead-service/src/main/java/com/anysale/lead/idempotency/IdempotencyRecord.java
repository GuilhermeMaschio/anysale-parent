package com.anysale.lead.idempotency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="idempotency_record",
        uniqueConstraints = @UniqueConstraint(name="uk_idem",
                columnNames={"operation","resource_id","idempotency_key"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyRecord {
    @Id @GeneratedValue
    private UUID id;

    @Column(nullable=false, length=100) private String operation;
    @Column(name="resource_id")         private UUID resourceId;
    @Column(name="idempotency_key", nullable=false, length=128) private String idempotencyKey;

    @Column(name="request_hash", nullable=false, length=64) private String requestHash;
    @Column(name="status_code", nullable=false)              private int statusCode;

    @Column(name="content_type", length=100) private String contentType;
    @Column(name="etag", length=100)         private String etag;
    @Column(name="location")                 private String location;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="expires_at")                 private Instant expiresAt;
}
