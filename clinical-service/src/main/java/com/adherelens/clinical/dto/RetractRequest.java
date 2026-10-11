package com.adherelens.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RetractRequest(@NotBlank @Size(max = 200) String reason) {
}
