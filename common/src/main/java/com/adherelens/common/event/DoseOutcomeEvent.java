package com.adherelens.common.event;

import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.DoseStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Published once a dose slot reaches a final status, whether by a response
 * or by the missed-dose detector when the window closes.
 * sideEffectCode is a coded value only, never free text.
 */
public record DoseOutcomeEvent(
        UUID doseSlotId,
        UUID hospitalId,
        UUID patientId,
        UUID medicationId,
        CareSetting careSetting,
        DoseStatus status,
        Instant dueAt,
        Instant respondedAt,
        String sideEffectCode
) {
}
