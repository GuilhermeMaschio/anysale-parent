package com.anysale.lead.aplication.ai;

import com.anysale.lead.domain.model.Interaction;
import com.anysale.lead.domain.model.Lead;

import java.util.List;

public interface LeadAiAssistant {

    LeadAiDraft analyzeConversation(Lead lead, List<Interaction> interactions);
}
