package com.anysale.notification.application;

import com.anysale.notification.adapters.in.messaging.LeadUpdatedListener;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.adapters.out.lead.LeadInteractionClient;
import com.anysale.notification.adapters.out.whatsapp.WhatsAppCloudApiClient;
import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WhatsAppOutboundServiceTest {

    @Mock
    private WhatsAppCloudApiClient whatsAppCloudApiClient;

    @Mock
    private LeadInteractionClient leadInteractionClient;

    @InjectMocks
    private WhatsAppOutboundService service;

    @Test
    void sendsTextMessageAndRecordsOutboundNotificationWhenLeadIdIsPresent() {
        UUID leadId = UUID.randomUUID();
        when(whatsAppCloudApiClient.sendTextMessage("5541999999999", "Mensagem outbound"))
                .thenReturn(new WhatsAppSendMessageResponse(
                        "whatsapp",
                        List.of(new WhatsAppSendMessageResponse.Contact("5541999999999", "5541999999999")),
                        List.of(new WhatsAppSendMessageResponse.Message("wamid.outbound.001", "accepted"))
                ));

        SendWhatsAppMessageResponse response = service.sendTextMessage(new SendWhatsAppMessageRequest(
                leadId,
                "5541999999999",
                "Mensagem outbound"
        ));

        assertThat(response.leadId()).isEqualTo(leadId);
        assertThat(response.waId()).isEqualTo("5541999999999");
        assertThat(response.messageId()).isEqualTo("wamid.outbound.001");
        assertThat(response.status()).isEqualTo("SENT");
        verify(leadInteractionClient).recordWhatsAppOutbound(
                leadId,
                "Mensagem outbound",
                "wamid.outbound.001"
        );
        assertThat(LeadUpdatedListener.byLead(leadId))
                .anySatisfy(entry -> assertThat(entry).contains("whatsapp.outbound messageId=wamid.outbound.001"));
    }
}
