-- Inpatient doses: one alert per dose slot for the nurses. Ids only, no personal data.
CREATE TABLE nurse_alerts (
    id               UUID PRIMARY KEY,
    dose_slot_id     UUID        NOT NULL,
    hospital_id      UUID        NOT NULL,
    patient_id       UUID        NOT NULL,
    regimen_id       UUID        NOT NULL,
    due_at           TIMESTAMPTZ NOT NULL,
    window_closes_at TIMESTAMPTZ NOT NULL,
    status           VARCHAR(20) NOT NULL,
    resolved_status  VARCHAR(20),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT nurse_alerts_slot_unique UNIQUE (dose_slot_id),
    CONSTRAINT nurse_alerts_status_check CHECK (status IN ('OPEN', 'RESOLVED'))
);

CREATE INDEX idx_nurse_alerts_hospital_status_due ON nurse_alerts (hospital_id, status, due_at);

-- Outpatient doses: a few bounded reminder attempts per dose slot.
CREATE TABLE reminders (
    id               UUID PRIMARY KEY,
    dose_slot_id     UUID        NOT NULL,
    hospital_id      UUID        NOT NULL,
    patient_id       UUID        NOT NULL,
    attempt_no       INT         NOT NULL,
    scheduled_for    TIMESTAMPTZ NOT NULL,
    window_closes_at TIMESTAMPTZ NOT NULL,
    status           VARCHAR(20) NOT NULL,
    sent_at          TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT reminders_attempt_unique UNIQUE (dose_slot_id, attempt_no),
    CONSTRAINT reminders_status_check CHECK (status IN ('PENDING', 'SENT', 'CANCELLED', 'FAILED'))
);

CREATE INDEX idx_reminders_status_scheduled ON reminders (status, scheduled_for);

-- Remembers which dose slots already reached a final status, so a dose-due event that
-- arrives late (events from two topics are not ordered against each other) is ignored.
CREATE TABLE slot_outcomes (
    dose_slot_id UUID PRIMARY KEY,
    status       VARCHAR(20) NOT NULL,
    received_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
