package com.adherelens.common.event;

import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.DoseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published once a dose slot reaches a final status, whether by a response or by the
 * missed-dose detector when the window closes. sideEffectCode is a coded value only.
 * Delivery is at-least-once, so consumers must treat doseSlotId as the idempotency key.
 */
public record DoseOutcomeEvent(
        UUID doseSlotId,
        UUID hospitalId,
        UUID patientId,
        UUID regimenId,
        CareSetting careSetting,
        DoseStatus status,
        Instant dueAt,
        Instant respondedAt,
        String sideEffectCode
) {
}
