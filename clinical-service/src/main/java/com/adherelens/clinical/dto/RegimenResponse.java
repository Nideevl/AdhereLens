package com.adherelens.clinical.dto;

import com.adherelens.clinical.domain.RegimenStatus;
import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RegimenResponse(
        UUID id,
        UUID patientId,
        UUID prescribedBy,
        String drugName,
        Formulation formulation,
        BigDecimal doseAmount,
        DoseUnit doseUnit,
        List<LocalTime> doseTimes,
        int dosesPerDay,
        LocalDate startDate,
        LocalDate endDate,
        int graceWindowMinutes,
        RegimenStatus status,
        LocalDate discontinuedOn
) {
}
