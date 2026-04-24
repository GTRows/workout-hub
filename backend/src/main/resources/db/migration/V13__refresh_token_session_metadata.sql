-- V13__refresh_token_session_metadata.sql
-- Adds user-agent and last-used metadata to refresh_tokens so the
-- /api/users/me/sessions list can show the caller which browsers and
-- devices currently hold a valid session, and let them revoke them.

ALTER TABLE refresh_tokens
    ADD COLUMN user_agent    VARCHAR(500),
    ADD COLUMN last_used_at  TIMESTAMPTZ;
