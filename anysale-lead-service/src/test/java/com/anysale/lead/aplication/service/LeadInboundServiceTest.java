package com.anysale.lead.aplication.service;

import com.anysale.lead.adapters.in.rest.dto.IncomingMessageRequest;
import com.anysale.lead.adapters.in.rest.dto.LeadResponseDto;
import com.anysale.lead.adapters.out.messaging.LeadEventPublisher;
import com.anysale.lead.adapters.out.persistence.InteractionJpaRepository;
import com.anysale.lead.adapters.out.persistence.LeadJpaRepository;
import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;
import com.anysale.lead.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadInboundServiceTest {

    @Mock
    private LeadJpaRepository leadRepository;

    @Mock
    private InteractionJpaRepository interactionRepository;

    @Mock
    private LeadEventPublisher leadEventPublisher;

    @Mock
    private LeadAiService leadAiService;

    @Mock
    private TenantContext tenantContext;

    @InjectMocks
    private LeadInboundService service;

    @Test
    void ignoresDuplicatedExternalMessage() {
        IncomingMessageRequest request = new IncomingMessageRequest(
                "41999999999",
                "Guilherme",
                "Quero saber mais",
                "WHATSAPP",
                "msg-duplicada"
        );

        Interaction interaction = new Interaction();
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setName("Lead duplicado");
        interaction.setLead(lead);
        when(interactionRepository.findByChannelAndExternalMessageId("WHATSAPP", "msg-duplicada"))
                .thenReturn(Optional.of(interaction));
        when(leadRepository.findByIdWithTags(lead.getId())).thenReturn(Optional.of(lead));

        LeadResponseDto response = service.execute(request);

        verify(leadRepository, never()).save(any(Lead.class));
        verify(interactionRepository, never()).save(any(Interaction.class));
        verify(leadEventPublisher, never()).publishLeadCreated(any(Lead.class));
        verify(leadEventPublisher, never()).publishLeadUpdated(any(Lead.class), any());
        assertThat(response.getId()).isEqualTo(lead.getId());
    }

}
