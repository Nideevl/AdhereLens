ALTER TABLE regimens ADD COLUMN schedule_effective_from TIMESTAMPTZ;
UPDATE regimens SET schedule_effective_from = created_at;
ALTER TABLE regimens ALTER COLUMN schedule_effective_from SET NOT NULL;

-- One row per changed field. All rows from one edit share a change_id, like the files in one git commit.
CREATE TABLE regimen_audit (
    id              UUID PRIMARY KEY,
    change_id       UUID         NOT NULL,
    hospital_id     UUID         NOT NULL,
    regimen_id      UUID         NOT NULL REFERENCES regimens (id),
    changed_by      UUID         NOT NULL,
    changed_by_name VARCHAR(200),
    changed_by_role VARCHAR(20)  NOT NULL,
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    field           VARCHAR(50)  NOT NULL,
    old_value       VARCHAR(200),
    new_value       VARCHAR(200),
    reason          VARCHAR(200)
);

CREATE INDEX idx_regimen_audit_regimen ON regimen_audit (regimen_id, changed_at DESC);

-- The history can only grow: nobody, including the application, can edit or delete a row.
CREATE FUNCTION prevent_audit_change() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'regimen_audit is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER regimen_audit_append_only
    BEFORE UPDATE OR DELETE ON regimen_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_change();
