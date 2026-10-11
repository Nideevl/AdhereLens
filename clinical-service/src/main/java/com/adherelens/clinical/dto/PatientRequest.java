package com.adherelens.clinical.dto;

import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Used for both create and update. conditionCode is a coded value, never free text. */
public record PatientRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 30) String phone,
        @Size(max = 30) String guardianPhone,
        @NotNull @Past LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotNull CareSetting careSetting,
        @NotBlank @Size(max = 50) @Pattern(regexp = "[A-Z0-9_]+", message = "must be upper-case letters, digits or underscore, e.g. TYPE2_DIABETES") String conditionCode
) {
}
