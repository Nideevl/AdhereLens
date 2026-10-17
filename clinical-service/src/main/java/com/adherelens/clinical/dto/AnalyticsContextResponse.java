package com.adherelens.clinical.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * What analytics may know about a regimen. De-identified at the source: age is a 10-year bucket
 * measured at the start of the regimen, never a date of birth. No name, phone or patient id.
 */
public record AnalyticsContextResponse(
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
