-- V2__users_and_profile.sql
-- Authoritative user identity and per-user profile. Users log in against
-- `users`; personal attributes (height, weight at signup, health notes, ...)
-- live in `user_profile` so the identity row stays small and read-hot.

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_role_chk     CHECK (role IN ('USER', 'ADMIN'))
);

CREATE UNIQUE INDEX idx_users_email_lower ON users (LOWER(email));

CREATE TABLE user_profile (
    user_id         UUID PRIMARY KEY
                        REFERENCES users(id) ON DELETE CASCADE,
    height_cm       INTEGER,
    weight_kg       NUMERIC(5,2),
    birth_date      DATE,
    gender          VARCHAR(20),
    health_notes    TEXT,
    goals           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
