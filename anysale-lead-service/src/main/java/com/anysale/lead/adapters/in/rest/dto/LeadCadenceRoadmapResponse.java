package com.anysale.lead.adapters.in.rest.dto;

import java.util.List;

public record LeadCadenceRoadmapResponse(LeadCadenceResponse cadence, List<CadenceStepResponse> steps) { }
