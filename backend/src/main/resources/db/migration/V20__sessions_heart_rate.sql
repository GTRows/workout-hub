-- V20__sessions_heart_rate.sql
-- Imported workouts from .fit files include average heart rate; we keep
-- it on the session row instead of a separate table because it's a
-- single per-session metric that travels with the workout.

ALTER TABLE workout_sessions
    ADD COLUMN heart_rate_avg_bpm SMALLINT
        CHECK (heart_rate_avg_bpm IS NULL OR (heart_rate_avg_bpm > 0 AND heart_rate_avg_bpm <= 250));
