package com.adherelens.common.event;

import com.adherelens.common.model.SideEffect;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a doctor or nurse reports a side effect, and again (retracted = true) when
 * it is retracted. Ids and a coded value only. Consumers treat reportId as the idempotency key
 * and let the latest event for a reportId win.
 */
public record SideEffectEvent(
        UUID reportId,
        UUID hospitalId,
        UUID patientId,
        UUID regimenId,
        SideEffect sideEffect,
        boolean retracted,
        Instant reportedAt
) {
}
