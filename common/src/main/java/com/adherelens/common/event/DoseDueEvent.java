package com.adherelens.common.event;

import com.adherelens.common.model.CareSetting;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a scheduled dose becomes due. Carries only internal ids, no personal data.
 * Delivery is at-least-once, so consumers must treat doseSlotId as the idempotency key.
 */
public record DoseDueEvent(
        UUID doseSlotId,
        UUID hospitalId,
        UUID patientId,
        UUID regimenId,
        CareSetting careSetting,
        Instant dueAt,
        Instant windowClosesAt
) {
}
