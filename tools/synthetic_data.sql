-- SYNTHETIC demo data for the analytics charts. Everything inserted here is flagged
-- synthetic = true and the screens say so. Safe to run again: it replaces its own rows first.
--   docker compose exec -T postgres psql -U adherelens -d adherelens < tools/synthetic_data.sql
-- Needs the services to have run once (so the analytics tables exist) and Hospital A / B to exist.
-- Drug names are obviously made up (DEMO_DRUG_*), so nothing here reads like clinical advice.
--
-- Injected patterns (so the charts have something to show):
--   1. Drop-off: outpatient adherence falls over the days of therapy. DEMO_DRUG_B falls fastest.
--   2. Inpatients stay near 97% with no drop-off (kept separate from outpatients).
--   3. Hospital comparison: DEMO_DRUG_A at the higher dose (hospitals 3 and 4) has lower adherence.
--   4. Side effects: DEMO_DRUG_A causes dizziness mostly in older patients, more at the higher dose.
--      DEMO_DRUG_B causes nausea mostly in younger women.

SELECT setseed(0.42);

DELETE FROM analytics.dose_facts WHERE synthetic;
DELETE FROM analytics.side_effect_facts WHERE synthetic;

CREATE TEMP TABLE syn_hospital AS
SELECT row_number() OVER (ORDER BY code) AS n, id
FROM identity.hospitals WHERE code IN ('HOSP_A', 'HOSP_B')
UNION ALL SELECT 3, '00000000-0000-4000-8000-000000000003'::uuid
UNION ALL SELECT 4, '00000000-0000-4000-8000-000000000004'::uuid;

CREATE TEMP TABLE syn_drug (name text, dose_lo numeric, dose_hi numeric, decay numeric);
INSERT INTO syn_drug VALUES ('DEMO_DRUG_A', 10, 20, 22), ('DEMO_DRUG_B', 5, 5, 9), ('DEMO_DRUG_C', 50, 50, 40);

-- 300 outpatients and 30 inpatients per hospital and drug (enough that one hospital's age and gender groups pass the small-group limit)
CREATE TEMP TABLE syn_patient AS
SELECT h.n AS hosp_n, h.id AS hospital_id, d.name AS drug, g AS pn,
       CASE WHEN g <= 300 THEN 'OUTPATIENT' ELSE 'INPATIENT' END AS care_setting,
       md5('syn-' || h.n || d.name || g) AS pseudo,
       CASE WHEN random() < 0.5 THEN 'FEMALE' ELSE 'MALE' END AS gender,
       (ARRAY['10-19','20-29','30-39','40-49','50-59','60-69','70-79'])[1 + floor(random() * 7)::int] AS age_bucket,
       CASE WHEN d.dose_lo <> d.dose_hi AND h.n >= 3 THEN d.dose_hi ELSE d.dose_lo END AS dose,
       d.decay,
       md5('reg-' || h.n || d.name || g)::uuid AS regimen_id
FROM syn_hospital h CROSS JOIN syn_drug d CROSS JOIN generate_series(1, 330) g;

INSERT INTO analytics.dose_facts
  (dose_slot_id, hospital_id, patient_pseudo, regimen_id, care_setting, status, due_at, day_of_therapy,
   drug_name, formulation, dose_amount, dose_unit, age_bucket, gender, condition_code, synthetic)
SELECT md5(random()::text || clock_timestamp()::text || p.pseudo || day)::uuid, p.hospital_id, p.pseudo, p.regimen_id,
       p.care_setting,
       CASE WHEN r.u < r.p_taken THEN (CASE WHEN random() < 0.8 THEN 'HIT' ELSE 'DELAYED' END)
            ELSE (CASE WHEN random() < 0.85 THEN 'MISSED' ELSE 'REFUSED' END) END,
       now() - ((30 - day) || ' days')::interval, day,
       p.drug, 'TABLET', p.dose, 'MG', p.age_bucket, p.gender, 'HYPERTENSION', TRUE
FROM syn_patient p
CROSS JOIN generate_series(1, 30) day
CROSS JOIN LATERAL (
  SELECT random() AS u,
         CASE WHEN p.care_setting = 'INPATIENT' THEN 0.97
              ELSE greatest(0.15, (0.93 - CASE WHEN p.drug = 'DEMO_DRUG_A' AND p.dose = 20 THEN 0.10 ELSE 0 END)
                                  * exp(-(day - 1) / p.decay)) END AS p_taken
) r;

-- Side effects: one report per affected patient, on a random day of therapy
INSERT INTO analytics.side_effect_facts
  (report_id, hospital_id, patient_pseudo, regimen_id, side_effect, reported_at, day_of_therapy,
   drug_name, age_bucket, gender, condition_code, synthetic)
SELECT md5(random()::text || p.pseudo || e.effect)::uuid, p.hospital_id, p.pseudo, p.regimen_id, e.effect,
       now() - ((30 - (3 + floor(random() * 20)::int)) || ' days')::interval, 3 + floor(random() * 20)::int,
       p.drug, p.age_bucket, p.gender, 'HYPERTENSION', TRUE
FROM syn_patient p
JOIN LATERAL (
  SELECT 'DIZZINESS' AS effect,
         CASE WHEN p.drug = 'DEMO_DRUG_A' THEN (CASE WHEN p.age_bucket IN ('60-69','70-79') THEN 0.35 ELSE 0.06 END)
                                               * (CASE WHEN p.dose = 20 THEN 1.6 ELSE 1 END)
              ELSE 0.03 END AS prob
  UNION ALL
  SELECT 'NAUSEA',
         CASE WHEN p.drug = 'DEMO_DRUG_B' THEN (CASE WHEN p.gender = 'FEMALE' AND p.age_bucket IN ('20-29','30-39') THEN 0.40 ELSE 0.07 END)
              ELSE 0.03 END
  UNION ALL
  SELECT 'HEADACHE', 0.05
) e ON random() < e.prob
WHERE p.care_setting = 'OUTPATIENT';

SELECT 'dose_facts' AS table_name, count(*) AS synthetic_rows FROM analytics.dose_facts WHERE synthetic
UNION ALL SELECT 'side_effect_facts', count(*) FROM analytics.side_effect_facts WHERE synthetic;
