package com.adherelens.clinical.dto;

import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;
import com.adherelens.common.model.Gender;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * One dosing rule. Age is in whole years, both ends inclusive. gender, minWeightKg, maxWeightKg
 * and strengthPerUnit are optional. strengthPerUnit uses the same unit as doseUnit
 * (for example 10 mg per tablet), and is only used to work out how many tablets or ml to give.
 */
public record DrugRuleRequest(
        @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Z0-9_]+", message = "must be upper-case letters, digits or underscore, e.g. METFORMIN") String drugName,
        @NotNull Formulation formulation,
        @NotNull @Min(0) @Max(150) Integer minAgeYears,
        @NotNull @Min(0) @Max(150) Integer maxAgeYears,
        Gender gender,
        @DecimalMin("0.3") @DecimalMax("500") @Digits(integer = 3, fraction = 2) BigDecimal minWeightKg,
        @DecimalMin("0.3") @DecimalMax("500") @Digits(integer = 3, fraction = 2) BigDecimal maxWeightKg,
        @NotNull @DecimalMin("0.001") BigDecimal doseAmount,
        @NotNull DoseUnit doseUnit,
        @DecimalMin("0.001") BigDecimal strengthPerUnit,
        @NotEmpty @Size(max = 6) List<@NotNull LocalTime> doseTimes
) {
}
