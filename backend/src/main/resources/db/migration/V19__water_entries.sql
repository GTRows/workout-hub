-- V19__water_entries.sql
-- Tiny per-user water log. We chose individual rows over a single
-- "today's total" so the user can undo a sip without reconstructing
-- the running sum.

CREATE TABLE water_entries (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL
                     REFERENCES users(id) ON DELETE CASCADE,
    ml           INTEGER      NOT NULL CHECK (ml > 0 AND ml <= 5000),
    consumed_at  TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_water_entries_user_date
    ON water_entries (user_id, consumed_at DESC);
