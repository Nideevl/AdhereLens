package com.adherelens.clinical.dto;

import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;

import java.math.BigDecimal;
import java.time.Instant;

/** What the no-login patient page may show. No name, no ids: the link itself is the credential. */
public record PatientDoseView(
        String drugName,
        Formulation formulation,
        BigDecimal doseAmount,
        DoseUnit doseUnit,
        Instant dueAt,
        Instant windowClosesAt,
        SlotStatus status,
        boolean canRecord
) {
}
