package com.anysale.lead.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByOperationAndResourceIdAndIdempotencyKey(String op, UUID resourceId, String key);
    long deleteByExpiresAtBefore(Instant t);
}

