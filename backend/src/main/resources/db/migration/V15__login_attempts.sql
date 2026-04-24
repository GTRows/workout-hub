-- V15__login_attempts.sql
-- Record of every login attempt for rate-limiting / brute-force lockout.
-- Indexed on (email, attempted_at) so the lockout service can answer
-- "how many failures for this email in the last window" cheaply.

CREATE TABLE login_attempts (
    id           UUID PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    success      BOOLEAN      NOT NULL,
    attempted_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_login_attempts_email_time
    ON login_attempts (email, attempted_at DESC);
