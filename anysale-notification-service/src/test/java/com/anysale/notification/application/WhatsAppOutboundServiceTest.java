package com.anysale.notification.application;

import com.anysale.notification.adapters.in.messaging.LeadUpdatedListener;
import com.anysale.notification.adapters.in.rest.dto.SendSuggestedWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageRequest;
import com.anysale.notification.adapters.in.rest.dto.SendWhatsAppMessageResponse;
import com.anysale.notification.adapters.out.lead.LeadInteractionClient;
import com.anysale.notification.adapters.out.lead.LeadQueryClient;
import com.anysale.notification.adapters.out.lead.dto.LeadContactSnapshot;
import com.anysale.notification.adapters.out.whatsapp.WhatsAppCloudApiClient;
import com.anysale.notification.adapters.out.whatsapp.dto.WhatsAppSendMessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppOutboundServiceTest {

    @Mock
    private WhatsAppCloudApiClient whatsAppCloudApiClient;

    @Mock
    private LeadInteractionClient leadInteractionClient;

    @Mock
    private LeadQueryClient leadQueryClient;

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

    @Test
    void sendsSuggestedMessageUsingLeadPhoneWhenRequestDoesNotProvideDestination() {
        UUID leadId = UUID.randomUUID();
        when(leadQueryClient.getLead(leadId))
                .thenReturn(new LeadContactSnapshot(
                        leadId,
                        "5541999999999",
                        "Oi! Posso te mandar algumas opcoes."
                ));
        when(whatsAppCloudApiClient.sendTextMessage("5541999999999", "Oi! Posso te mandar algumas opcoes."))
                .thenReturn(new WhatsAppSendMessageResponse(
                        "whatsapp",
                        List.of(new WhatsAppSendMessageResponse.Contact("5541999999999", "5541999999999")),
                        List.of(new WhatsAppSendMessageResponse.Message("wamid.suggested.001", "accepted"))
                ));

        SendWhatsAppMessageResponse response = service.sendSuggestedMessage(
                new SendSuggestedWhatsAppMessageRequest(leadId, null)
        );

        assertThat(response.leadId()).isEqualTo(leadId);
        assertThat(response.to()).isEqualTo("5541999999999");
        assertThat(response.messageId()).isEqualTo("wamid.suggested.001");
        verify(leadInteractionClient).recordWhatsAppOutbound(
                leadId,
                "Oi! Posso te mandar algumas opcoes.",
                "wamid.suggested.001"
        );
    }

    @Test
    void rejectsSuggestedMessageWhenLeadDoesNotHaveDraftYet() {
        UUID leadId = UUID.randomUUID();
        when(leadQueryClient.getLead(leadId))
                .thenReturn(new LeadContactSnapshot(
                        leadId,
                        "5541999999999",
                        "   "
                ));

        assertThatThrownBy(() -> service.sendSuggestedMessage(
                new SendSuggestedWhatsAppMessageRequest(leadId, null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Lead does not have a suggested reply yet");
    }
}
