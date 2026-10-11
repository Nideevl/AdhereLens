package com.adherelens.clinical.dto;

import com.adherelens.common.model.DoseUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Every field is optional: only the fields sent are changed. The drug and formulation cannot be
 * edited, because a different drug is a new regimen. reason is stored in the history only.
 */
public record RegimenUpdateRequest(
        @DecimalMin("0.001") BigDecimal doseAmount,
        DoseUnit doseUnit,
        @Size(min = 1, max = 6) List<@NotNull LocalTime> doseTimes,
        LocalDate endDate,
        @Min(5) @Max(240) Integer graceWindowMinutes,
        @Size(max = 200) String reason
) {
}
