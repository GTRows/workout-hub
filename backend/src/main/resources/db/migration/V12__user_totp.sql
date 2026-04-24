-- V12__user_totp.sql
-- Per-user TOTP secret for two-factor authentication. The secret is
-- stored base32-encoded and can be disabled without destroying the
-- secret so the user can re-enable without re-scanning a QR code.
-- backup_codes_hash stores a comma-separated list of BCrypt hashes of
-- one-time recovery codes; each hash is consumed (row rewritten) once
-- used.

CREATE TABLE user_totp (
    user_id            UUID PRIMARY KEY
                           REFERENCES users(id) ON DELETE CASCADE,
    secret_base32      VARCHAR(64)  NOT NULL,
    enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    backup_codes_hash  TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
