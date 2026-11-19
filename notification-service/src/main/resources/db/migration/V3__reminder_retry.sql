-- Retry support for reminders. send_tries counts failed send tries; next_attempt_at is when the
-- scheduler may try again (equal to scheduled_for until the first failure). claimed_at marks a
-- reminder that an instance is sending right now, so a crashed instance's claim can be recovered.
-- last_error holds an exception class name only, never a message, so no phone number can end up here.
ALTER TABLE reminders ADD COLUMN send_tries INT NOT NULL DEFAULT 0;
ALTER TABLE reminders ADD COLUMN next_attempt_at TIMESTAMPTZ;
UPDATE reminders SET next_attempt_at = scheduled_for;
ALTER TABLE reminders ALTER COLUMN next_attempt_at SET NOT NULL;
ALTER TABLE reminders ADD COLUMN claimed_at TIMESTAMPTZ;
ALTER TABLE reminders ADD COLUMN last_error VARCHAR(200);

ALTER TABLE reminders DROP CONSTRAINT reminders_status_check;
ALTER TABLE reminders ADD CONSTRAINT reminders_status_check
    CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'CANCELLED', 'FAILED'));

DROP INDEX idx_reminders_status_scheduled;
CREATE INDEX idx_reminders_status_next_attempt ON reminders (status, next_attempt_at);
