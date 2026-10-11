package com.adherelens.identity.dto;

import com.adherelens.common.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull Role role,
        @Size(max = 100) String specialty
) {
}
