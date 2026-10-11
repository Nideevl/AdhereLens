package com.adherelens.clinical.dto;

import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** drugName is a coded value such as METFORMIN. graceWindowMinutes defaults to 60 when omitted. */
public record RegimenRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Z0-9_]+", message = "must be upper-case letters, digits or underscore, e.g. METFORMIN") String drugName,
        @NotNull Formulation formulation,
        @NotNull @DecimalMin("0.001") BigDecimal doseAmount,
        @NotNull DoseUnit doseUnit,
        @NotEmpty @Size(max = 6) List<@NotNull LocalTime> doseTimes,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @Min(5) @Max(240) Integer graceWindowMinutes
) {
}
