package com.adherelens.clinical.dto;

import com.adherelens.clinical.domain.SideEffectStatus;
import com.adherelens.common.model.SideEffect;

import java.time.Instant;
import java.util.UUID;

public record SideEffectResponse(
        UUID id,
        UUID patientId,
        UUID regimenId,
        SideEffect sideEffect,
        UUID reportedBy,
        String reportedByName,
        String reportedByRole,
        Instant reportedAt,
        SideEffectStatus status,
        Instant retractedAt,
        String retractReason
) {
}
