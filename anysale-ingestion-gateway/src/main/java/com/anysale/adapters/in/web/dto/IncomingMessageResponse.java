package com.anysale.adapters.in.web.dto;

public record IncomingMessageResponse(
        String status,
        String normalizedPhone
) {
}