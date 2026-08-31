package com.anysale.lead.adapters.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ManagedUserCreateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72) String temporaryPassword,
        @NotBlank @Pattern(regexp = "ADMIN|SALES_MANAGER|SALES_AGENT") String role
) {
}
