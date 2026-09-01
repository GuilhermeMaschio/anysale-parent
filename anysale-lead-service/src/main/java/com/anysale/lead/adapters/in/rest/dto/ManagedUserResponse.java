package com.anysale.lead.adapters.in.rest.dto;

import java.time.Instant;

public record ManagedUserResponse(
        String id,
        String firstName,
        String lastName,
        String email,
        String role,
        boolean enabled,
        Instant createdAt
) {
}
