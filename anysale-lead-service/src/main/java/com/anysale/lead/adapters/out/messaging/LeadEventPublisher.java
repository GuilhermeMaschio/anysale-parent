package com.anysale.lead.adapters.out.messaging;

import com.anysale.contracts.event.LeadUpdatedEvent;
import com.anysale.lead.domain.model.Lead;

public interface LeadEventPublisher {
    // usado hoje no createLead(...)
    void publishLeadCreated(Lead lead);

    // usado hoje no changeStage(...) com DTO pós-commit
    void publishLeadUpdated(LeadUpdatedEvent event);

    // usado hoje no attachSuggestions(...)
    void publishLeadUpdated(Lead lead, String reason);

}
