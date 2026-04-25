-- V25__theme_preference.sql
-- Per-user theme override; null = follow OS preference, 'light' or
-- 'dark' force a specific theme. Stored as a short string so it travels
-- in /api/users/me without a separate endpoint.

ALTER TABLE user_profile
    ADD COLUMN theme_preference VARCHAR(8)
        CHECK (theme_preference IS NULL OR theme_preference IN ('light', 'dark'));
