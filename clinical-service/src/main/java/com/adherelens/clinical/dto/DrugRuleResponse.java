package com.adherelens.clinical.dto;

import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import com.adherelens.common.model.Gender;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record DrugRuleResponse(
        UUID id,
        String drugName,
        Formulation formulation,
        int minAgeYears,
        int maxAgeYears,
        Gender gender,
        BigDecimal minWeightKg,
        BigDecimal maxWeightKg,
        BigDecimal doseAmount,
        DoseUnit doseUnit,
        BigDecimal strengthPerUnit,
        List<LocalTime> doseTimes,
        boolean active
) {
}
