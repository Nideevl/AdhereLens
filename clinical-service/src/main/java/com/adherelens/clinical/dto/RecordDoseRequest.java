package com.adherelens.clinical.dto;

import jakarta.validation.constraints.NotNull;

public record RecordDoseRequest(@NotNull RecordOutcome outcome) {
}
