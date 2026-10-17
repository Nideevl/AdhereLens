package com.adherelens.analytics.service;

import com.adherelens.analytics.dto.AnalyticsDtos.DayPoint;
import com.adherelens.analytics.dto.AnalyticsDtos.DrugRow;
import com.adherelens.analytics.dto.AnalyticsDtos.HospitalRow;
import com.adherelens.analytics.dto.AnalyticsDtos.Result;
import com.adherelens.analytics.dto.AnalyticsDtos.SideEffectRow;
import com.adherelens.analytics.dto.AnalyticsDtos.SummaryRow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Every query reads de-identified fact tables only. hospitalId null means all hospitals together
 * (hospital names are never returned); otherwise the query is limited to that hospital.
 * Any group with fewer distinct patients than minGroupSize is hidden before it leaves this class.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsQueryService {

    private static final String TAKEN = "SUM(CASE WHEN status IN ('HIT', 'DELAYED') THEN 1 ELSE 0 END)";
    private static final String ON_TIME = "SUM(CASE WHEN status = 'HIT' THEN 1 ELSE 0 END)";

    private final NamedParameterJdbcTemplate jdbc;
    private final int minGroupSize;
    private final String labelSalt;

    public AnalyticsQueryService(NamedParameterJdbcTemplate jdbc,
                                 @Value("${adherelens.analytics.min-group-size}") int minGroupSize,
                                 @Value("${adherelens.analytics.pseudonym-secret}") String labelSalt) {
        this.jdbc = jdbc;
        this.minGroupSize = minGroupSize;
        this.labelSalt = labelSalt;
    }

    public Result<String> drugs(UUID hospitalId) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT DISTINCT drug_name FROM dose_facts WHERE 1 = 1" + hospital(hospitalId, p) + " ORDER BY drug_name";
        return wrap(jdbc.query(sql, p, (rs, i) -> rs.getString(1)), hospitalId);
    }

    public Result<SummaryRow> summary(UUID hospitalId, String drugName) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT care_setting, COUNT(*) AS doses, COUNT(DISTINCT patient_pseudo) AS patients, "
                + "SUM(CASE WHEN status = 'HIT' THEN 1 ELSE 0 END) AS hit, "
                + "SUM(CASE WHEN status = 'DELAYED' THEN 1 ELSE 0 END) AS delayed, "
                + "SUM(CASE WHEN status = 'MISSED' THEN 1 ELSE 0 END) AS missed, "
                + "SUM(CASE WHEN status = 'REFUSED' THEN 1 ELSE 0 END) AS refused "
                + "FROM dose_facts WHERE 1 = 1" + hospital(hospitalId, p) + drug(drugName, p) + " GROUP BY care_setting ORDER BY care_setting";
        List<SummaryRow> rows = jdbc.query(sql, p, (rs, i) -> {
            long doses = rs.getLong("doses");
            long patients = rs.getLong("patients");
            long hit = rs.getLong("hit");
            long delayed = rs.getLong("delayed");
            boolean hide = patients < minGroupSize;
            return hide
                    ? new SummaryRow(rs.getString("care_setting"), 0, 0, 0, 0, 0, 0, null, null, true)
                    : new SummaryRow(rs.getString("care_setting"), doses, patients, hit, delayed, rs.getLong("missed"),
                            rs.getLong("refused"), pct(hit + delayed, doses), pct(hit, doses), false);
        });
        return wrap(rows, hospitalId);
    }

    public Result<DayPoint> adherenceByDay(UUID hospitalId, String careSetting, String drugName, int maxDay) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("cs", careSetting).addValue("maxDay", maxDay);
        String sql = "SELECT day_of_therapy AS day, COUNT(*) AS doses, COUNT(DISTINCT patient_pseudo) AS patients, "
                + TAKEN + " AS taken, " + ON_TIME + " AS on_time "
                + "FROM dose_facts WHERE care_setting = :cs AND day_of_therapy BETWEEN 1 AND :maxDay"
                + hospital(hospitalId, p) + drug(drugName, p)
                + " GROUP BY day_of_therapy ORDER BY day_of_therapy";
        List<DayPoint> rows = jdbc.query(sql, p, (rs, i) -> {
            long doses = rs.getLong("doses");
            long patients = rs.getLong("patients");
            return patients < minGroupSize
                    ? new DayPoint(rs.getInt("day"), 0, 0, null, null, true)
                    : new DayPoint(rs.getInt("day"), doses, patients, pct(rs.getLong("taken"), doses),
                            pct(rs.getLong("on_time"), doses), false);
        });
        return wrap(rows, hospitalId);
    }

    public Result<DrugRow> adherenceByDrug(UUID hospitalId, String careSetting) {
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("cs", careSetting);
        String sql = "SELECT drug_name, COUNT(*) AS doses, COUNT(DISTINCT patient_pseudo) AS patients, "
                + TAKEN + " AS taken, " + ON_TIME + " AS on_time "
                + "FROM dose_facts WHERE care_setting = :cs" + hospital(hospitalId, p)
                + " GROUP BY drug_name ORDER BY COUNT(*) DESC";
        List<DrugRow> rows = jdbc.query(sql, p, (rs, i) -> {
            long doses = rs.getLong("doses");
            long patients = rs.getLong("patients");
            return patients < minGroupSize
                    ? new DrugRow(rs.getString("drug_name"), 0, 0, null, null, true)
                    : new DrugRow(rs.getString("drug_name"), doses, patients, pct(rs.getLong("taken"), doses),
                            pct(rs.getLong("on_time"), doses), false);
        });
        return wrap(rows, hospitalId);
    }

    /** Share of each age and gender group that had a side effect reported. Retracted reports are ignored. */
    public Result<SideEffectRow> sideEffects(UUID hospitalId, String drugName) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String scope = hospital(hospitalId, p) + drug(drugName, p);
        String sql = "WITH cohort AS (SELECT age_bucket, gender, COUNT(DISTINCT patient_pseudo) AS cohort_patients "
                + "FROM dose_facts WHERE 1 = 1" + scope + " GROUP BY age_bucket, gender), "
                + "reported AS (SELECT age_bucket, gender, side_effect, COUNT(DISTINCT patient_pseudo) AS reporting "
                + "FROM side_effect_facts WHERE NOT retracted" + scope + " GROUP BY age_bucket, gender, side_effect) "
                + "SELECT r.age_bucket, r.gender, r.side_effect, r.reporting, c.cohort_patients "
                + "FROM reported r JOIN cohort c ON c.age_bucket = r.age_bucket AND c.gender = r.gender "
                + "ORDER BY r.reporting DESC, r.age_bucket, r.gender";
        List<SideEffectRow> rows = jdbc.query(sql, p, (rs, i) -> {
            long reporting = rs.getLong("reporting");
            long cohort = rs.getLong("cohort_patients");
            boolean hide = reporting < minGroupSize || cohort < minGroupSize;
            return hide
                    ? new SideEffectRow(rs.getString("age_bucket"), rs.getString("gender"), rs.getString("side_effect"), 0, 0, null, true)
                    : new SideEffectRow(rs.getString("age_bucket"), rs.getString("gender"), rs.getString("side_effect"),
                            reporting, cohort, pct(reporting, cohort), false);
        });
        return wrap(rows, hospitalId);
    }

    /**
     * Same drug at each hospital, split by dose. Hospitals appear only as a short code that cannot be
     * traced back to the hospital; the caller's own hospital is marked mine.
     */
    public Result<HospitalRow> hospitalComparison(UUID myHospitalId, String careSetting, String drugName) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("cs", careSetting).addValue("drug", drugName).addValue("salt", labelSalt);
        String sql = "SELECT hospital_id, 'H-' || substr(md5(hospital_id::text || :salt), 1, 4) AS label, "
                + "dose_amount, dose_unit, COUNT(*) AS doses, COUNT(DISTINCT patient_pseudo) AS patients, "
                + TAKEN + " AS taken, " + ON_TIME + " AS on_time "
                + "FROM dose_facts WHERE care_setting = :cs AND drug_name = :drug "
                + "GROUP BY hospital_id, dose_amount, dose_unit ORDER BY label, dose_amount";
        List<HospitalRow> rows = jdbc.query(sql, p, (rs, i) -> {
            long doses = rs.getLong("doses");
            long patients = rs.getLong("patients");
            boolean mine = myHospitalId != null && myHospitalId.equals(rs.getObject("hospital_id", UUID.class));
            return patients < minGroupSize
                    ? new HospitalRow(rs.getString("label"), mine, rs.getBigDecimal("dose_amount"), rs.getString("dose_unit"), 0, 0, null, null, true)
                    : new HospitalRow(rs.getString("label"), mine, rs.getBigDecimal("dose_amount"), rs.getString("dose_unit"),
                            doses, patients, pct(rs.getLong("taken"), doses), pct(rs.getLong("on_time"), doses), false);
        });
        return wrap(rows, null);
    }

    private <T> Result<T> wrap(List<T> rows, UUID hospitalId) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT EXISTS (SELECT 1 FROM dose_facts WHERE synthetic" + hospital(hospitalId, p) + ")";
        Boolean synthetic = jdbc.queryForObject(sql, p, Boolean.class);
        return new Result<>(rows, minGroupSize, Boolean.TRUE.equals(synthetic));
    }

    private static String hospital(UUID hospitalId, MapSqlParameterSource p) {
        if (hospitalId == null) {
            return "";
        }
        p.addValue("hospital", hospitalId);
        return " AND hospital_id = :hospital";
    }

    private static String drug(String drugName, MapSqlParameterSource p) {
        if (drugName == null || drugName.isBlank()) {
            return "";
        }
        p.addValue("drug", drugName);
        return " AND drug_name = :drug";
    }

    private static double pct(long part, long whole) {
        return whole == 0 ? 0.0 : Math.round(1000.0 * part / whole) / 10.0;
    }
}
