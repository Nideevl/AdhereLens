CREATE TABLE hospitals (
    id           UUID PRIMARY KEY,
    code         VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    api_key_hash VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE staff (
    id            UUID PRIMARY KEY,
    hospital_id   UUID REFERENCES hospitals (id),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    specialty     VARCHAR(100),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT staff_role_check
        CHECK (role IN ('PLATFORM_ADMIN', 'HOSPITAL_ADMIN', 'DOCTOR', 'NURSE')),
    CONSTRAINT staff_hospital_check
        CHECK ((role = 'PLATFORM_ADMIN' AND hospital_id IS NULL)
            OR (role <> 'PLATFORM_ADMIN' AND hospital_id IS NOT NULL))
);

CREATE INDEX idx_staff_hospital ON staff (hospital_id);
