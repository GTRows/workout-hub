-- V6__body_metrics_and_supplements.sql
-- Periodic body snapshots and the per-user supplement stack. Metrics are
-- append-only (one row per recorded_date per user); edits rewrite the row.
-- Supplements carry an active flag so retired items stay in history.

CREATE TABLE body_metrics (
    id                  UUID PRIMARY KEY,
    user_id             UUID        NOT NULL
                            REFERENCES users(id) ON DELETE CASCADE,
    recorded_date       DATE        NOT NULL,
    weight_kg           NUMERIC(5,2),
    body_fat_percent    NUMERIC(4,1),
    waist_cm            NUMERIC(5,1),
    chest_cm            NUMERIC(5,1),
    arm_cm              NUMERIC(5,1),
    thigh_cm            NUMERIC(5,1),
    photo_url           VARCHAR(500),
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT body_metrics_user_date_unique UNIQUE (user_id, recorded_date),
    CONSTRAINT body_metrics_bodyfat_chk
        CHECK (body_fat_percent IS NULL OR body_fat_percent BETWEEN 0 AND 100)
);

CREATE INDEX idx_body_metrics_recorded_date ON body_metrics (recorded_date);
CREATE INDEX idx_body_metrics_user_date
    ON body_metrics (user_id, recorded_date DESC);

CREATE TABLE supplements (
    id              UUID PRIMARY KEY,
    user_id         UUID        NOT NULL
                        REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(120) NOT NULL,
    dosage          VARCHAR(60),
    timing          VARCHAR(20)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT supplements_timing_chk CHECK (
        timing IN ('morning', 'pre_workout', 'post_workout', 'evening', 'with_meal', 'other')
    )
);

CREATE INDEX idx_supplements_user_active ON supplements (user_id) WHERE active = TRUE;
