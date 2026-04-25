-- V21__webhook_tokens.sql
-- Per-user webhook tokens for relay endpoints (smart scale, etc).
-- We rotate by deleting and re-issuing rather than versioning the row,
-- so the URL is the only credential the relay holds.

CREATE TABLE webhook_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(64)  NOT NULL UNIQUE,
    purpose     VARCHAR(32)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_webhook_tokens_user_purpose
    ON webhook_tokens (user_id, purpose);
