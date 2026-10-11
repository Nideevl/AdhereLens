-- One row per reported side effect. Only a doctor or nurse can report one, so every row
-- names the clinician. A wrong entry is retracted (never edited or deleted), so the
-- record of what was reported stays complete.
CREATE TABLE side_effect_reports (
    id               UUID PRIMARY KEY,
    hospital_id      UUID         NOT NULL,
    patient_id       UUID         NOT NULL REFERENCES patients (id),
    regimen_id       UUID         NOT NULL REFERENCES regimens (id),
    side_effect      VARCHAR(40)  NOT NULL,
    reported_by      UUID         NOT NULL,
    reported_by_name VARCHAR(200),
    reported_by_role VARCHAR(20)  NOT NULL,
    reported_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    retracted_by     UUID,
    retracted_by_name VARCHAR(200),
    retracted_at     TIMESTAMPTZ,
    retract_reason   VARCHAR(200),
    CONSTRAINT side_effect_reports_status_check CHECK (status IN ('ACTIVE', 'RETRACTED')),
    CONSTRAINT side_effect_reports_retract_check CHECK (status = 'ACTIVE' OR retract_reason IS NOT NULL)
);

CREATE INDEX idx_side_effect_reports_patient ON side_effect_reports (patient_id, reported_at DESC);
CREATE INDEX idx_side_effect_reports_regimen ON side_effect_reports (regimen_id);
