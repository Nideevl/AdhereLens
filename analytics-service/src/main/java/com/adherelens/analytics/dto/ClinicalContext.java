package com.adherelens.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** What clinical-service returns for a regimen. Already de-identified at the source. */
public record ClinicalContext(
        UUID regimenId,
        String drugName,
        String formulation,
        BigDecimal doseAmount,
        String doseUnit,
        LocalDate startDate,
        String ageBucket,
        String gender,
        String conditionCode
) {
}
