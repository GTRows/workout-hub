-- V11__supplement_reminder_time.sql
-- Adds an optional daily reminder-time column to supplements so the
-- notifications.ReminderJob can fire a per-user-defined time trigger.

ALTER TABLE supplements
    ADD COLUMN reminder_time TIME;

CREATE INDEX idx_supplements_user_reminder_time
    ON supplements (user_id, reminder_time)
    WHERE reminder_time IS NOT NULL AND active = TRUE;
