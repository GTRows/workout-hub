-- V10__push_subscriptions.sql
-- Browser Push API subscription endpoints per user. endpoint is unique
-- globally so re-subscribing on the same device updates the row instead
-- of inserting a duplicate. p256dh + auth are the client-generated keys
-- required to sign notifications later (PHASE 7 trigger work).

CREATE TABLE push_subscriptions (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL
                    REFERENCES users(id) ON DELETE CASCADE,
    endpoint    TEXT        NOT NULL,
    p256dh_key  VARCHAR(255) NOT NULL,
    auth_key    VARCHAR(255) NOT NULL,
    user_agent  VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT push_subscriptions_endpoint_unique UNIQUE (endpoint)
);

CREATE INDEX idx_push_subscriptions_user ON push_subscriptions (user_id);
