package com.anysale.lead.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component @EnableScheduling @RequiredArgsConstructor @Slf4j
public class IdempotencyCleanup {
    private final IdempotencyRecordRepository repo;

    // a cada 6h remove registros expirados
    @Scheduled(fixedDelayString = "PT6H")
    public void cleanup() {
        long n = repo.deleteByExpiresAtBefore(Instant.now());
        if (n > 0) log.info("Idempotency cleanup removed {} records", n);
    }
}
