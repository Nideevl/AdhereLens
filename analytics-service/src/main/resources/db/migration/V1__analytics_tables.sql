-- De-identified facts. No names, phones or dates of birth ever land here.
-- patient_pseudo is a keyed hash of the patient id, so one patient can be counted across rows
-- without being identifiable. Age is kept only as a 10-year bucket.

-- What each regimen looked like when its first outcome arrived (looked up once, then reused).
CREATE TABLE regimen_context (
    regimen_id     UUID PRIMARY KEY,
    hospital_id    UUID         NOT NULL,
    drug_name      VARCHAR(100) NOT NULL,
    formulation    VARCHAR(20)  NOT NULL,
    dose_amount    NUMERIC(10, 3) NOT NULL,
    dose_unit      VARCHAR(10)  NOT NULL,
    start_date     DATE         NOT NULL,
    age_bucket     VARCHAR(10)  NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    condition_code VARCHAR(50)  NOT NULL,
    fetched_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- One row per dose slot with a final status. A later event for the same slot replaces it.
CREATE TABLE dose_facts (
    dose_slot_id   UUID PRIMARY KEY,
    hospital_id    UUID         NOT NULL,
    patient_pseudo VARCHAR(64)  NOT NULL,
    regimen_id     UUID         NOT NULL,
    care_setting   VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    due_at         TIMESTAMPTZ  NOT NULL,
    day_of_therapy INT          NOT NULL,
    drug_name      VARCHAR(100) NOT NULL,
    formulation    VARCHAR(20)  NOT NULL,
    dose_amount    NUMERIC(10, 3) NOT NULL,
    dose_unit      VARCHAR(10)  NOT NULL,
    age_bucket     VARCHAR(10)  NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    condition_code VARCHAR(50)  NOT NULL,
    synthetic      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT dose_facts_status_check CHECK (status IN ('HIT', 'DELAYED', 'MISSED', 'REFUSED')),
    CONSTRAINT dose_facts_setting_check CHECK (care_setting IN ('INPATIENT', 'OUTPATIENT'))
);

CREATE INDEX idx_dose_facts_scope ON dose_facts (hospital_id, care_setting, drug_name, day_of_therapy);
CREATE INDEX idx_dose_facts_drug ON dose_facts (drug_name, care_setting);

-- One row per clinician-reported side effect. retracted rows stay but are ignored by every query.
CREATE TABLE side_effect_facts (
    report_id      UUID PRIMARY KEY,
    hospital_id    UUID         NOT NULL,
    patient_pseudo VARCHAR(64)  NOT NULL,
    regimen_id     UUID         NOT NULL,
    side_effect    VARCHAR(40)  NOT NULL,
    reported_at    TIMESTAMPTZ  NOT NULL,
    day_of_therapy INT          NOT NULL,
    drug_name      VARCHAR(100) NOT NULL,
    age_bucket     VARCHAR(10)  NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    condition_code VARCHAR(50)  NOT NULL,
    retracted      BOOLEAN      NOT NULL DEFAULT FALSE,
    synthetic      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_side_effect_facts_scope ON side_effect_facts (hospital_id, drug_name, age_bucket, gender);
