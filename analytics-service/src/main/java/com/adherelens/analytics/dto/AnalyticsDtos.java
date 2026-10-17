package com.adherelens.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result shapes. A row with suppressed = true belongs to a group with too few patients: its
 * numbers are zero or null and must not be shown. takenRate = HIT + DELAYED out of all doses.
 * onTimeRate = HIT out of all doses. Rates are percentages with one decimal.
 */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record Result<T>(List<T> rows, int minGroupSize, boolean includesSynthetic) {
    }

    public record DayPoint(int day, long doses, long patients, Double takenRate, Double onTimeRate, boolean suppressed) {
    }

    public record DrugRow(String drugName, long doses, long patients, Double takenRate, Double onTimeRate, boolean suppressed) {
    }

    public record SideEffectRow(String ageBucket, String gender, String sideEffect, long patientsReporting,
                                long cohortPatients, Double ratePercent, boolean suppressed) {
    }

    public record HospitalRow(String label, boolean mine, BigDecimal doseAmount, String doseUnit, long doses,
                              long patients, Double takenRate, Double onTimeRate, boolean suppressed) {
    }

    public record SummaryRow(String careSetting, long doses, long patients, long hit, long delayed, long missed,
                             long refused, Double takenRate, Double onTimeRate, boolean suppressed) {
    }
}
