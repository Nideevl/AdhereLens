package com.adherelens.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateHospitalRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+", message = "must be upper-case letters, digits or underscore") String code,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(max = 200) String adminFullName,
        @NotBlank @Size(min = 8, max = 72) String adminPassword
) {
}
