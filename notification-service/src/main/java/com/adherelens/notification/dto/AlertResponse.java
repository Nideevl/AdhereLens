package com.adherelens.notification.dto;

import com.adherelens.notification.domain.AlertStatus;

import java.time.Instant;
import java.util.UUID;

/** Ids only. Patient names stay in clinical-service; the screen joins them using patientId. */
public record AlertResponse(
        UUID id,
        UUID doseSlotId,
        UUID patientId,
        UUID regimenId,
        Instant dueAt,
        Instant windowClosesAt,
        AlertStatus status,
        String resolvedStatus
) {
}
