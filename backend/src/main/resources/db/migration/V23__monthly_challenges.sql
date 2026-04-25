-- V23__monthly_challenges.sql
-- One challenge per calendar month, scoped globally so every user
-- competes on the same prompt. The (year_month) UNIQUE constraint
-- guards against duplicate challenges; admins replace by upserting.

CREATE TABLE monthly_challenges (
    id              UUID PRIMARY KEY,
    year_month      CHAR(7)      NOT NULL UNIQUE,
    name_tr         TEXT         NOT NULL,
    name_en         TEXT         NOT NULL,
    description_tr  TEXT,
    description_en  TEXT,
    rule_type       VARCHAR(32)  NOT NULL,
    threshold       INTEGER      NOT NULL CHECK (threshold > 0),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
