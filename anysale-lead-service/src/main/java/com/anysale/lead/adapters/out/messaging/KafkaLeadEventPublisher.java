package com.anysale.lead.adapters.out.messaging;

import com.anysale.contracts.event.LeadCreatedEvent;
import com.anysale.contracts.event.LeadUpdatedEvent;
import com.anysale.lead.domain.model.Lead;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaLeadEventPublisher implements LeadEventPublisher {

    private static final String TOPIC_CREATED = "lead.created";
    private static final String TOPIC_UPDATED = "lead.updated";

    private final KafkaTemplate<String, Object> kafka;

    public KafkaLeadEventPublisher(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @Override
    public void publishLeadCreated(Lead lead) {
        LeadCreatedEvent evt = toCreatedEvent(lead);
        // chave = id do lead; valor = DTO simples (Jackson serializa)
        kafka.send(TOPIC_CREATED, evt.getId().toString(), evt);
    }

    @Override
    public void publishLeadUpdated(LeadUpdatedEvent event) {
        kafka.send(TOPIC_UPDATED, event.getId().toString(), event);
    }

    @Override
    public void publishLeadUpdated(Lead lead, String reason) {
        LeadUpdatedEvent evt = toUpdatedEvent(lead, reason);
        kafka.send(TOPIC_UPDATED, evt.getId().toString(), evt);
    }

    // --------- mapeadores privados ---------
    private static LeadCreatedEvent toCreatedEvent(Lead lead) {
        LeadCreatedEvent evt = new LeadCreatedEvent();
        evt.setId(lead.getId());
        evt.setName(lead.getName());
        evt.setEmail(lead.getEmail());
        evt.setPhone(lead.getPhone());
        evt.setSource(lead.getSource());
        evt.setDesiredCategory(lead.getDesiredCategory());
        evt.setDesiredTags(lead.getDesiredTags());
        evt.setStage(lead.getStage());
        return evt;
    }

    private static LeadUpdatedEvent toUpdatedEvent(Lead lead, String reason) {
        return new LeadUpdatedEvent(lead.getId(), lead.getStage(), reason);
    }
}
