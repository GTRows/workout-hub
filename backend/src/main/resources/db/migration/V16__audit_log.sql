-- V16__audit_log.sql
-- Append-only record of every admin-surface mutation so we can answer
-- "who changed what and when".

CREATE TABLE audit_log (
    id           UUID PRIMARY KEY,
    actor_id     UUID         NOT NULL
                     REFERENCES users(id) ON DELETE CASCADE,
    action       VARCHAR(80)  NOT NULL,
    target_type  VARCHAR(60)  NOT NULL,
    target_id    UUID,
    payload_json TEXT,
    at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_at ON audit_log (at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id, at DESC);
