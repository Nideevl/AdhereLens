-- Each reminder attempt is either a text message or a phone call.
ALTER TABLE reminders ADD COLUMN channel VARCHAR(10) NOT NULL DEFAULT 'SMS';
ALTER TABLE reminders ADD CONSTRAINT reminders_channel_check CHECK (channel IN ('SMS', 'CALL'));
