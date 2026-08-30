package com.anysale.lead.adapters.in.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record LeadCadenceResponse(UUID id, UUID leadId, UUID playbookId, String playbookName, String status,
                                  int nextPosition, Instant nextActionAt, Instant startedAt, Instant pausedAt, Instant completedAt) { }
