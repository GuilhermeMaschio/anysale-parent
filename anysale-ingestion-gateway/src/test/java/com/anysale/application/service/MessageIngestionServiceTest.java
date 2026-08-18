package com.anysale.application.service;

import com.anysale.adapters.in.web.dto.IncomingMessageRequest;
import com.anysale.adapters.in.web.dto.IncomingMessageResponse;
import com.anysale.application.model.LeadSnapshot;
import com.anysale.application.port.out.LeadGatewayPort;
import com.anysale.application.port.out.MessageEventPublisherPort;
import com.anysale.domain.model.IncomingMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageIngestionServiceTest {

    private static final java.util.UUID LEAD_ID = java.util.UUID.fromString("b5c1ce42-fbec-4f99-b822-4f4c9ef74f12");

    @Mock
    private LeadGatewayPort leadGatewayPort;

    @Mock
    private MessageEventPublisherPort messageEventPublisherPort;

    @InjectMocks
    private MessageIngestionService service;

    @Test
    void normalizesPhoneAndDispatchesToGatewayAndPublisher() {
        IncomingMessageRequest request = new IncomingMessageRequest(
                "+55 (41) 99999-9999",
                "Guilherme",
                "Quero saber mais",
                "WHATSAPP",
                "msg-1"
        );

        LeadSnapshot leadSnapshot = new LeadSnapshot();
        leadSnapshot.setId(LEAD_ID);
        leadSnapshot.setName("Guilherme");
        leadSnapshot.setPhone("5541999999999");
        leadSnapshot.setSource("WHATSAPP");
        leadSnapshot.setDesiredTags(java.util.List.of());
        leadSnapshot.setStage("CONTACTED");
        leadSnapshot.setLastMessage("Quero saber mais");

        when(leadGatewayPort.createOrUpdateLeadFromIncomingMessage(any())).thenReturn(Mono.just(leadSnapshot));

        IncomingMessageResponse response = service.execute(request).block();

        assertThat(response.status()).isEqualTo("RECEIVED");
        assertThat(response.normalizedPhone()).isEqualTo("5541999999999");
        assertThat(response.leadId()).isEqualTo(LEAD_ID);
        assertThat(response.lead().stage()).isEqualTo("CONTACTED");

        ArgumentCaptor<IncomingMessage> messageCaptor = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(leadGatewayPort).createOrUpdateLeadFromIncomingMessage(messageCaptor.capture());
        verify(messageEventPublisherPort).publishIncomingMessage(messageCaptor.getValue());

        IncomingMessage dispatched = messageCaptor.getValue();
        assertThat(dispatched.getPhone()).isEqualTo("5541999999999");
        assertThat(dispatched.getLeadName()).isEqualTo("Guilherme");
        assertThat(dispatched.getMessage()).isEqualTo("Quero saber mais");
        assertThat(dispatched.getChannel()).isEqualTo("WHATSAPP");
        assertThat(dispatched.getExternalMessageId()).isEqualTo("msg-1");
    }
}
