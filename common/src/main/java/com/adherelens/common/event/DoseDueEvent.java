package com.adherelens.common.event;

import com.adherelens.common.model.CareSetting;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a scheduled dose becomes due. Carries no personal data:
 * only internal ids. Contact details are looked up inside the hospital-private
 * side by whichever service needs to notify someone.
 */
public record DoseDueEvent(
        UUID doseSlotId,
        UUID hospitalId,
        UUID patientId,
        UUID medicationId,
        CareSetting careSetting,
        Instant dueAt,
        Instant windowClosesAt
) {
}
