package com.adherelens.clinical.dto;

import com.adherelens.common.model.CareSetting;
import com.adherelens.common.model.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String fullName,
        String phone,
        String guardianPhone,
        LocalDate dateOfBirth,
        Gender gender,
        CareSetting careSetting,
        String conditionCode,
        boolean active,
        BigDecimal weightKg
) {
}
