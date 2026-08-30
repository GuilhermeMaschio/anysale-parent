package com.anysale.lead.adapters.in.rest.dto;

import java.time.Instant;
public record AiSkillResponse(String profile, String label, String content, boolean customized, Instant updatedAt) { }
