-- A hospital's own dosing rule for one drug. The system only SUGGESTS from these rows;
-- a doctor confirms or edits, and the regimen stores the final values.
-- Age is in whole years, both ends inclusive. Weight and gender are optional conditions.
-- strength_per_unit is in the same unit as dose_unit (e.g. mg per tablet, mg per ml).
CREATE TABLE drug_rules (
    id                UUID PRIMARY KEY,
    hospital_id       UUID           NOT NULL,
    drug_name         VARCHAR(100)   NOT NULL,
    formulation       VARCHAR(20)    NOT NULL,
    min_age_years     INT            NOT NULL,
    max_age_years     INT            NOT NULL,
    gender            VARCHAR(10),
    min_weight_kg     NUMERIC(5, 2),
    max_weight_kg     NUMERIC(5, 2),
    dose_amount       NUMERIC(10, 3) NOT NULL,
    dose_unit         VARCHAR(10)    NOT NULL,
    strength_per_unit NUMERIC(10, 3),
    created_by        UUID           NOT NULL,
    active            BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT drug_rules_age_check CHECK (min_age_years >= 0 AND max_age_years <= 150 AND min_age_years <= max_age_years),
    CONSTRAINT drug_rules_gender_check CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT drug_rules_weight_check CHECK (
        (min_weight_kg IS NULL OR min_weight_kg > 0)
        AND (max_weight_kg IS NULL OR max_weight_kg <= 500)
        AND (min_weight_kg IS NULL OR max_weight_kg IS NULL OR min_weight_kg <= max_weight_kg)),
    CONSTRAINT drug_rules_dose_positive CHECK (dose_amount > 0),
    CONSTRAINT drug_rules_strength_positive CHECK (strength_per_unit IS NULL OR strength_per_unit > 0)
);

CREATE INDEX idx_drug_rules_hospital_drug ON drug_rules (hospital_id, drug_name);

CREATE TABLE drug_rule_dose_times (
    rule_id   UUID NOT NULL REFERENCES drug_rules (id) ON DELETE CASCADE,
    dose_time TIME NOT NULL,
    PRIMARY KEY (rule_id, dose_time)
);
