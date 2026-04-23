-- V4__workout_plans_days_exercises.sql
-- Template plan the user configures. A plan has days; each day holds an
-- ordered list of exercises with target reps/weight. The actual performed
-- workouts land in V5 (workout_sessions + session_sets).

CREATE TABLE workout_plans (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,
    name        VARCHAR(120) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workout_plans_user        ON workout_plans (user_id);
CREATE UNIQUE INDEX idx_workout_plans_one_active_per_user
    ON workout_plans (user_id) WHERE is_active = TRUE;

CREATE TABLE workout_days (
    id                      UUID PRIMARY KEY,
    plan_id                 UUID        NOT NULL
                                REFERENCES workout_plans(id) ON DELETE CASCADE,
    day_of_week             SMALLINT    NOT NULL,
    name                    VARCHAR(120) NOT NULL,
    focus                   VARCHAR(20)  NOT NULL,
    estimated_duration_min  INTEGER,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT workout_days_day_of_week_chk CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT workout_days_focus_chk CHECK (
        focus IN ('push', 'pull', 'legs', 'cardio', 'core', 'full_body', 'rest')
    ),
    CONSTRAINT workout_days_plan_day_unique UNIQUE (plan_id, day_of_week)
);

CREATE INDEX idx_workout_days_plan ON workout_days (plan_id);

CREATE TABLE workout_day_exercises (
    id                  UUID PRIMARY KEY,
    workout_day_id      UUID        NOT NULL
                            REFERENCES workout_days(id) ON DELETE CASCADE,
    exercise_id         UUID        NOT NULL
                            REFERENCES exercises(id) ON DELETE RESTRICT,
    order_index         INTEGER     NOT NULL,
    target_sets         INTEGER     NOT NULL,
    target_reps_min     INTEGER,
    target_reps_max     INTEGER,
    target_weight_kg    NUMERIC(6,2),
    rest_seconds        INTEGER,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT workout_day_exercises_order_unique UNIQUE (workout_day_id, order_index)
);

CREATE INDEX idx_workout_day_exercises_day      ON workout_day_exercises (workout_day_id);
CREATE INDEX idx_workout_day_exercises_exercise ON workout_day_exercises (exercise_id);
