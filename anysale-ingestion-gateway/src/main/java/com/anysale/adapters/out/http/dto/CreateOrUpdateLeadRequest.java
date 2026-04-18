package com.anysale.adapters.out.http.dto;

public record CreateOrUpdateLeadRequest(
        String phone,
        String leadName,
        String message,
        String channel,
        String externalMessageId
) {
}