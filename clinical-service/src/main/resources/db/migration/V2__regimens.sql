-- A regimen is one drug prescribed to one patient on a fixed daily schedule.
-- prescribed_by is a staff id from identity-service (no foreign key across services).
CREATE TABLE regimens (
    id                   UUID PRIMARY KEY,
    hospital_id          UUID           NOT NULL,
    patient_id           UUID           NOT NULL REFERENCES patients (id),
    prescribed_by        UUID           NOT NULL,
    drug_name            VARCHAR(100)   NOT NULL,
    formulation          VARCHAR(20)    NOT NULL,
    dose_amount          NUMERIC(10, 3) NOT NULL,
    dose_unit            VARCHAR(10)    NOT NULL,
    start_date           DATE           NOT NULL,
    end_date             DATE,
    grace_window_minutes INT            NOT NULL DEFAULT 60,
    status               VARCHAR(20)    NOT NULL,
    discontinued_on      DATE,
    created_at           TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT regimens_status_check CHECK (status IN ('ACTIVE', 'COMPLETED', 'DISCONTINUED')),
    CONSTRAINT regimens_dates_check CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT regimens_grace_check CHECK (grace_window_minutes BETWEEN 5 AND 240),
    CONSTRAINT regimens_dose_positive CHECK (dose_amount > 0)
);

CREATE INDEX idx_regimens_patient ON regimens (patient_id);
CREATE INDEX idx_regimens_hospital_status ON regimens (hospital_id, status);

CREATE TABLE regimen_dose_times (
    regimen_id UUID NOT NULL REFERENCES regimens (id) ON DELETE CASCADE,
    dose_time  TIME NOT NULL,
    PRIMARY KEY (regimen_id, dose_time)
);
