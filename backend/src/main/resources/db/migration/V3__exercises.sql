-- V3__exercises.sql
-- Master exercise catalog. Shared across all users and edited by ADMIN only.
-- Bilingual name (tr primary, en unique for search + external mapping).
-- Category / equipment / difficulty use app-level enums enforced by CHECK.

CREATE TABLE exercises (
    id                UUID PRIMARY KEY,
    name_tr           VARCHAR(120) NOT NULL,
    name_en           VARCHAR(120) NOT NULL,
    category          VARCHAR(20)  NOT NULL,
    equipment         VARCHAR(20)  NOT NULL,
    muscle_primary    VARCHAR(40)  NOT NULL,
    muscle_secondary  VARCHAR(40),
    description_tr    TEXT,
    form_tips         TEXT[]       NOT NULL DEFAULT '{}',
    common_mistakes   TEXT[]       NOT NULL DEFAULT '{}',
    image_url         VARCHAR(500),
    video_url         VARCHAR(500),
    difficulty        VARCHAR(20)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT exercises_name_en_unique UNIQUE (name_en),
    CONSTRAINT exercises_category_chk CHECK (
        category IN ('push', 'pull', 'legs', 'cardio', 'core', 'forearm')
    ),
    CONSTRAINT exercises_equipment_chk CHECK (
        equipment IN ('dumbbell', 'zbar', 'bodyweight', 'bar', 'wrist_tool', 'machine', 'other')
    ),
    CONSTRAINT exercises_difficulty_chk CHECK (
        difficulty IN ('beginner', 'intermediate', 'advanced')
    )
);

CREATE INDEX idx_exercises_category   ON exercises (category);
CREATE INDEX idx_exercises_equipment  ON exercises (equipment);
CREATE INDEX idx_exercises_difficulty ON exercises (difficulty);
CREATE INDEX idx_exercises_name_tr_lower ON exercises (LOWER(name_tr));
