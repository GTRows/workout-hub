-- V7__refresh_tokens.sql
-- Persistent record of issued refresh tokens so we can rotate them on each
-- /auth/refresh call. We store only the SHA-256 hash of the token (never
-- the token itself). The JWT carries its own signed expiration; the row
-- mirrors it so expiry + revocation checks are a simple SELECT.

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT refresh_tokens_token_hash_unique UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_user_active
    ON refresh_tokens (user_id) WHERE revoked = FALSE;
