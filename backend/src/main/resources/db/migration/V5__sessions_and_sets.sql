-- V5__sessions_and_sets.sql
-- Recorded workouts. workout_day_id is the plan slot the user set out to do
-- (nullable so ad-hoc sessions are allowed). session_sets captures every
-- performed set with actual weight/reps and optional RPE self-rating.

CREATE TABLE workout_sessions (
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL
                            REFERENCES users(id) ON DELETE CASCADE,
    workout_day_id      UUID
                            REFERENCES workout_days(id) ON DELETE SET NULL,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at            TIMESTAMPTZ,
    notes               TEXT,
    mood                SMALLINT,
    energy_level        SMALLINT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT workout_sessions_mood_chk         CHECK (mood IS NULL OR mood BETWEEN 1 AND 5),
    CONSTRAINT workout_sessions_energy_chk       CHECK (energy_level IS NULL OR energy_level BETWEEN 1 AND 5),
    CONSTRAINT workout_sessions_ended_after_started CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_workout_sessions_user_started
    ON workout_sessions (user_id, started_at DESC);
CREATE UNIQUE INDEX idx_workout_sessions_one_active_per_user
    ON workout_sessions (user_id) WHERE ended_at IS NULL;

CREATE TABLE session_sets (
    id              UUID PRIMARY KEY,
    session_id      UUID        NOT NULL
                        REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id     UUID        NOT NULL
                        REFERENCES exercises(id) ON DELETE RESTRICT,
    set_number      SMALLINT    NOT NULL,
    reps_done       SMALLINT    NOT NULL,
    weight_kg       NUMERIC(6,2),
    rpe             SMALLINT,
    completed       BOOLEAN     NOT NULL DEFAULT TRUE,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT session_sets_set_number_chk CHECK (set_number >= 1),
    CONSTRAINT session_sets_rpe_chk        CHECK (rpe IS NULL OR rpe BETWEEN 1 AND 10),
    CONSTRAINT session_sets_unique         UNIQUE (session_id, exercise_id, set_number)
);

CREATE INDEX idx_session_sets_session  ON session_sets (session_id);
CREATE INDEX idx_session_sets_exercise ON session_sets (exercise_id);
