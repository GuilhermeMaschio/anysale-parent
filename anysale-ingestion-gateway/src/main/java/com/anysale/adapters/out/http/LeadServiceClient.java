package com.anysale.adapters.out.http;

import com.anysale.adapters.out.http.dto.CreateOrUpdateLeadRequest;
import com.anysale.application.port.out.LeadGatewayPort;
import com.anysale.domain.model.IncomingMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class LeadServiceClient implements LeadGatewayPort {

    private final WebClient leadServiceWebClient;

    @Override
    public void createOrUpdateLeadFromIncomingMessage(IncomingMessage message) {
        CreateOrUpdateLeadRequest request = new CreateOrUpdateLeadRequest(
                message.getPhone(),
                message.getLeadName(),
                message.getMessage(),
                message.getChannel(),
                message.getExternalMessageId()
        );

        leadServiceWebClient.post()
                .uri("/v1/leads/incoming-message")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
