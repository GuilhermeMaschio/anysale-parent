package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

public record ConsolePreferenceRequest(
        @NotNull @Pattern(regexp = "light|dark") String colorTheme
) { }
