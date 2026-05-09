-- V28__rest_timer_schedules.sql
-- Server-side scheduled rest-timer push notifications.
--
-- Phase 34: when the user starts a rest interval inside a session, the
-- frontend posts a schedule row keyed by (user_id, session_id). A background
-- @Scheduled poller fires due rows through the existing NotificationDispatcher
-- so backgrounded tabs / locked screens still get the rest-over alert.
--
-- One in-flight schedule per (user, session) pair: re-scheduling replaces
-- the row via the unique constraint. dispatched_at acts as a sentinel; the
-- cleanup tick prunes rows where dispatched_at < now() - 1h.

CREATE TABLE rest_timer_schedules (
    id                 UUID PRIMARY KEY,
    user_id            UUID         NOT NULL,
    session_id         UUID         NOT NULL,
    fire_at            TIMESTAMPTZ  NOT NULL,
    localized_title    TEXT         NOT NULL,
    localized_body     TEXT         NOT NULL,
    click_url          TEXT         NOT NULL,
    dispatched_at      TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT rest_timer_schedules_user_session_unique UNIQUE (user_id, session_id)
);

CREATE INDEX idx_rest_timer_schedules_due
    ON rest_timer_schedules (fire_at)
    WHERE dispatched_at IS NULL;

CREATE INDEX idx_rest_timer_schedules_cleanup
    ON rest_timer_schedules (dispatched_at)
    WHERE dispatched_at IS NOT NULL;
