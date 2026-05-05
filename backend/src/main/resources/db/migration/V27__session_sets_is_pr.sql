-- V27__session_sets_is_pr.sql
-- Persists per-set PR detection on session_sets so the wire field newPr is durable
-- across detail re-read instead of a create-only flash flag. Closes the gap from
-- Phase 15-04 where the create response carried newPr=true but a subsequent GET
-- omitted it.
--
-- Schema-level uniqueness is intentionally NOT enforced. The application layer
-- (SessionSetsService) is the sole writer of is_pr and guarantees at most one
-- TRUE row per (user_id, exercise_id) on every mutation path. The schema permits
-- transient violations during update-path re-election (demote-then-promote runs
-- inside a single @Transactional service call).
--
-- Backfill semantic: per (user_id, exercise_id), mark the completed set with
-- the highest Epley one-rep-max as is_pr. The Epley expression is inlined since
-- existing migrations carry only inline SQL and Postgres numeric arithmetic on
-- the source column shape NUMERIC(6,2) is exact for this expression. Tie-break
-- on created_at ASC so the earliest set holding the best 1RM gets the badge,
-- matching PrDetector.beatsPriorBest strict-greater semantics (an equal Epley
-- does not beat the prior best, so the first-appearance set is the durable one).
-- The backfill restricts to ws.ended_at IS NOT NULL to mirror
-- SessionSetRepository.findHistoricalByUserAndExercise (finished-only history).

ALTER TABLE session_sets ADD COLUMN is_pr BOOLEAN NOT NULL DEFAULT false;

WITH ranked AS (
    SELECT
        ss.id AS set_id,
        ROW_NUMBER() OVER (
            PARTITION BY ws.user_id, ss.exercise_id
            ORDER BY
                CASE
                    WHEN ss.weight_kg IS NULL OR ss.reps_done <= 0 THEN NULL
                    WHEN ss.reps_done = 1 THEN ss.weight_kg
                    ELSE ss.weight_kg * (1 + ss.reps_done::numeric / 30)
                END DESC NULLS LAST,
                ss.created_at ASC
        ) AS rk
    FROM session_sets ss
    JOIN workout_sessions ws ON ws.id = ss.session_id
    WHERE ss.completed = TRUE
      AND ws.ended_at IS NOT NULL
)
UPDATE session_sets
SET is_pr = TRUE
FROM ranked
WHERE session_sets.id = ranked.set_id
  AND ranked.rk = 1;
