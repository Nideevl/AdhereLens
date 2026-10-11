ALTER TABLE patients
    ADD COLUMN weight_kg NUMERIC(5,2),
    ADD CONSTRAINT patients_weight_check CHECK (weight_kg IS NULL OR (weight_kg >= 0.3 AND weight_kg <= 500));
