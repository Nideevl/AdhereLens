package com.adherelens.clinical.dto;

import com.adherelens.clinical.domain.SlotStatus;
import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DoseSlotResponse(
        UUID id,
        UUID patientId,
        String patientName,
        String drugName,
        Formulation formulation,
        BigDecimal doseAmount,
        DoseUnit doseUnit,
        CareSetting careSetting,
        Instant dueAt,
        Instant windowClosesAt,
        SlotStatus status,
        Instant respondedAt
) {
}
