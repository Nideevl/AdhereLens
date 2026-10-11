-- One row per scheduled dose. care_setting is a snapshot taken when the slot is created,
-- so a later discharge does not rewrite history. version is for optimistic locking.
CREATE TABLE dose_slots (
    id                  UUID PRIMARY KEY,
    hospital_id         UUID        NOT NULL,
    patient_id          UUID        NOT NULL REFERENCES patients (id),
    regimen_id          UUID        NOT NULL REFERENCES regimens (id),
    care_setting        VARCHAR(20) NOT NULL,
    due_at              TIMESTAMPTZ NOT NULL,
    window_closes_at    TIMESTAMPTZ NOT NULL,
    status              VARCHAR(20) NOT NULL,
    responded_at        TIMESTAMPTZ,
    recorded_by         UUID,
    reported_by_role    VARCHAR(20),
    due_event_published BOOLEAN     NOT NULL DEFAULT FALSE,
    outcome_published   BOOLEAN     NOT NULL DEFAULT FALSE,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT dose_slots_unique UNIQUE (regimen_id, due_at),
    CONSTRAINT dose_slots_status_check
        CHECK (status IN ('PENDING', 'HIT', 'DELAYED', 'MISSED', 'REFUSED', 'CANCELLED')),
    CONSTRAINT dose_slots_care_setting_check CHECK (care_setting IN ('INPATIENT', 'OUTPATIENT'))
);

CREATE INDEX idx_dose_slots_hospital_status_due ON dose_slots (hospital_id, status, due_at);
