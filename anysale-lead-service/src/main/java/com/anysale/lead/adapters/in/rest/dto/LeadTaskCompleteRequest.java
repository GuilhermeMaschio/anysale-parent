package com.anysale.lead.adapters.in.rest.dto;
import jakarta.validation.constraints.*;
public record LeadTaskCompleteRequest(@NotBlank @Pattern(regexp="WHATSAPP_REPLIED|CALL_COMPLETED|NO_RESPONSE|PROPOSAL_SENT|MEETING_SCHEDULED|LOST|OTHER") String outcome, @Size(max=1000) String note) {}
