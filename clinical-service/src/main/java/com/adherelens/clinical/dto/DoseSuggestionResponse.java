package com.adherelens.clinical.dto;

import com.adherelens.common.model.DoseUnit;
import com.adherelens.common.model.Formulation;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * What the hospital's own rules suggest for one patient and drug. This is a suggestion only:
 * the doctor confirms or edits it when prescribing. weightKg is null when it is not on file,
 * and rules that need a weight are then left out and explained in notes, never guessed.
 */
public record DoseSuggestionResponse(
        UUID patientId,
        String drugName,
        int ageYears,
        BigDecimal weightKg,
        List<Suggestion> suggestions,
        List<String> notes
) {

    /** unitsToAdminister (tablets or ml) is null when the rule has no strengthPerUnit. */
    public record Suggestion(
            UUID ruleId,
            Formulation formulation,
            BigDecimal doseAmount,
            DoseUnit doseUnit,
            List<LocalTime> doseTimes,
            BigDecimal unitsToAdminister,
            String unitsLabel
    ) {
    }
}
