package com.anysale.lead.adapters.in.rest.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AiSkillRequest(@NotBlank @Size(max = 8000) String content) { }
