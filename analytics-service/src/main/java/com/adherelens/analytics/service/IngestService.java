package com.adherelens.analytics.service;

import com.adherelens.analytics.dto.ClinicalContext;
import com.adherelens.common.event.DoseOutcomeEvent;
import com.adherelens.common.event.SideEffectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Turns events into de-identified fact rows. Every write is an upsert on the event's own id,
 * so a repeated event changes nothing and a newer event for the same id replaces the old one.
 */
@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);
    private static final Set<String> FINAL_STATUSES = Set.of("HIT", "DELAYED", "MISSED", "REFUSED");

    private final JdbcTemplate jdbc;
    private final ClinicalClient clinicalClient;
    private final Pseudonymizer pseudonymizer;
    private final ZoneId zone;

    public IngestService(JdbcTemplate jdbc, ClinicalClient clinicalClient, Pseudonymizer pseudonymizer,
                         @Value("${adherelens.analytics.zone}") String zone) {
        this.jdbc = jdbc;
        this.clinicalClient = clinicalClient;
        this.pseudonymizer = pseudonymizer;
        this.zone = ZoneId.of(zone);
    }

    @Transactional
    public void onDoseOutcome(DoseOutcomeEvent event) {
        String status = event.status().name();
        if (!FINAL_STATUSES.contains(status)) {
            log.info("Ignoring dose outcome {} with status {}", event.doseSlotId(), status);
            return;
        }
        ClinicalContext ctx = contextFor(event.hospitalId(), event.regimenId());
        jdbc.update("""
                INSERT INTO dose_facts (dose_slot_id, hospital_id, patient_pseudo, regimen_id, care_setting, status,
                    due_at, day_of_therapy, drug_name, formulation, dose_amount, dose_unit, age_bucket, gender, condition_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (dose_slot_id) DO UPDATE SET status = EXCLUDED.status
                """,
                event.doseSlotId(), event.hospitalId(), pseudonymizer.patient(event.patientId()), event.regimenId(),
                event.careSetting().name(), status, utc(event.dueAt()), dayOfTherapy(ctx, event.dueAt()),
                ctx.drugName(), ctx.formulation(), ctx.doseAmount(), ctx.doseUnit(),
                ctx.ageBucket(), ctx.gender(), ctx.conditionCode());
    }

    @Transactional
    public void onSideEffect(SideEffectEvent event) {
        ClinicalContext ctx = contextFor(event.hospitalId(), event.regimenId());
        jdbc.update("""
                INSERT INTO side_effect_facts (report_id, hospital_id, patient_pseudo, regimen_id, side_effect, reported_at,
                    day_of_therapy, drug_name, age_bucket, gender, condition_code, retracted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (report_id) DO UPDATE SET retracted = EXCLUDED.retracted
                """,
                event.reportId(), event.hospitalId(), pseudonymizer.patient(event.patientId()), event.regimenId(),
                event.sideEffect().name(), utc(event.reportedAt()), dayOfTherapy(ctx, event.reportedAt()),
                ctx.drugName(), ctx.ageBucket(), ctx.gender(), ctx.conditionCode(), event.retracted());
    }

    /** Day 1 is the regimen's start date. */
    private int dayOfTherapy(ClinicalContext ctx, Instant when) {
        long days = ChronoUnit.DAYS.between(ctx.startDate(), when.atZone(zone).toLocalDate()) + 1;
        return (int) Math.max(1, days);
    }

    private static OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /** Reads the regimen's context from our own table, asking clinical-service only the first time. */
    private ClinicalContext contextFor(UUID hospitalId, UUID regimenId) {
        List<ClinicalContext> cached = jdbc.query(
                "SELECT regimen_id, drug_name, formulation, dose_amount, dose_unit, start_date, age_bucket, gender, condition_code "
                        + "FROM regimen_context WHERE regimen_id = ?",
                (rs, i) -> new ClinicalContext(rs.getObject("regimen_id", UUID.class), rs.getString("drug_name"),
                        rs.getString("formulation"), rs.getBigDecimal("dose_amount"), rs.getString("dose_unit"),
                        rs.getObject("start_date", java.time.LocalDate.class), rs.getString("age_bucket"),
                        rs.getString("gender"), rs.getString("condition_code")),
                regimenId);
        if (!cached.isEmpty()) {
            return cached.get(0);
        }
        ClinicalContext fresh = clinicalClient.context(hospitalId, regimenId);
        jdbc.update("""
                INSERT INTO regimen_context (regimen_id, hospital_id, drug_name, formulation, dose_amount, dose_unit,
                    start_date, age_bucket, gender, condition_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (regimen_id) DO NOTHING
                """,
                regimenId, hospitalId, fresh.drugName(), fresh.formulation(), fresh.doseAmount(), fresh.doseUnit(),
                fresh.startDate(), fresh.ageBucket(), fresh.gender(), fresh.conditionCode());
        return fresh;
    }
}
