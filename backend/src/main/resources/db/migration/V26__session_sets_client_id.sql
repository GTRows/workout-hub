-- V26__session_sets_client_id.sql
-- Adds an opt-in idempotency key to session_sets for the offline-first drainer.
-- client_set_id is a client-supplied UUID that allows the IndexedDB sync loop to
-- replay a POST /sets without risk of duplicate insertion. The column is nullable
-- so pre-V26 rows and clients that omit the key remain valid without any migration
-- of existing data.
-- The partial UNIQUE index enforces (session_id, client_set_id) uniqueness only
-- when the key is supplied, while allowing any number of NULL rows to coexist.

ALTER TABLE session_sets ADD COLUMN client_set_id UUID;

CREATE UNIQUE INDEX idx_session_sets_client_id_unique
    ON session_sets (session_id, client_set_id)
    WHERE client_set_id IS NOT NULL;
