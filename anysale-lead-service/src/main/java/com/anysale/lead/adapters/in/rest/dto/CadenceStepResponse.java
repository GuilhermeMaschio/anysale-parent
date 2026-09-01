package com.anysale.lead.adapters.in.rest.dto;

import java.util.UUID;

public record CadenceStepResponse(UUID id, int position, int delayMinutes, String title, String taskType, String priority, String note) { }
