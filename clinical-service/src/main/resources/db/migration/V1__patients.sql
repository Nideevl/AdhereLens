-- hospital_id is a plain UUID: it refers to identity.hospitals, which belongs to
-- another service, so there is deliberately no foreign key across services.
CREATE TABLE patients (
    id             UUID PRIMARY KEY,
    hospital_id    UUID         NOT NULL,
    full_name      VARCHAR(200) NOT NULL,
    phone          VARCHAR(30),
    guardian_phone VARCHAR(30),
    date_of_birth  DATE         NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    care_setting   VARCHAR(20)  NOT NULL,
    condition_code VARCHAR(50)  NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT patients_gender_check CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT patients_care_setting_check CHECK (care_setting IN ('INPATIENT', 'OUTPATIENT'))
);

CREATE INDEX idx_patients_hospital ON patients (hospital_id);
