-- Runs once, when the Postgres volume is first created. One schema per service.
-- Flyway also creates its own schema if missing, so this only makes a fresh clone start cleanly.
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS clinical;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS analytics;
