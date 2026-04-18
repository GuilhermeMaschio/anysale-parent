package com.anysale.domain.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IncomingMessage {

    private final String phone;
    private final String leadName;
    private final String message;
    private final String channel;
    private final String externalMessageId;
}