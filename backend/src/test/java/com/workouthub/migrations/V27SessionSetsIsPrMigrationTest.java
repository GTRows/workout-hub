package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class V27SessionSetsIsPrMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void isPrColumnExists() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT udt_name, is_nullable, column_default
                FROM information_schema.columns
                WHERE table_name = 'session_sets'
                  AND column_name = 'is_pr'
                """);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("udt_name")).isEqualTo("bool");
        assertThat(rows.get(0).get("is_nullable")).isEqualTo("NO");
        assertThat(rows.get(0).get("column_default")).isEqualTo("false");
    }

    @Test
    void newRowsDefaultToFalse() {
        UUID userId = seedUser();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = seedExercise();
        UUID setId = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id) VALUES (?, ?)",
                sessionId, userId);
        jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done) "
                        + "VALUES (?, ?, ?, ?, ?)",
                setId, sessionId, exerciseId, 1, 8);

        Boolean isPr = jdbc.queryForObject(
                "SELECT is_pr FROM session_sets WHERE id = ?",
                Boolean.class, setId);
        assertThat(isPr).isFalse();
    }

    @Test
    void backfillMarksTopEpleyRowPerUserExercisePair() {
        UUID userId = seedUser();
        UUID exerciseId = seedExercise();

        UUID s1 = UUID.randomUUID();
        UUID s2 = UUID.randomUUID();
        UUID set1 = UUID.randomUUID();
        UUID set2 = UUID.randomUUID();
        Instant now = Instant.now();

        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id, started_at, ended_at) "
                        + "VALUES (?, ?, ?, ?)",
                s1, userId,
                java.sql.Timestamp.from(now.minusSeconds(120)),
                java.sql.Timestamp.from(now.minusSeconds(60)));
        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id, started_at, ended_at) "
                        + "VALUES (?, ?, ?, ?)",
                s2, userId,
                java.sql.Timestamp.from(now.minusSeconds(60)),
                java.sql.Timestamp.from(now));
        // s1: 100kg x 5 -> Epley 116.667 (the winner)
        jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done, weight_kg, completed) "
                        + "VALUES (?, ?, ?, ?, ?, ?, TRUE)",
                set1, s1, exerciseId, 1, 5, new java.math.BigDecimal("100.00"));
        // s2: 95kg x 5 -> Epley 110.833
        jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done, weight_kg, completed) "
                        + "VALUES (?, ?, ?, ?, ?, ?, TRUE)",
                set2, s2, exerciseId, 1, 5, new java.math.BigDecimal("95.00"));

        // Re-run the backfill SQL so the assertion exercises the production statement
        // (V27 already executed against an empty session_sets table at app start).
        jdbc.update(
                """
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
                  AND ranked.rk = 1
                """);

        Boolean set1IsPr = jdbc.queryForObject(
                "SELECT is_pr FROM session_sets WHERE id = ?",
                Boolean.class, set1);
        Boolean set2IsPr = jdbc.queryForObject(
                "SELECT is_pr FROM session_sets WHERE id = ?",
                Boolean.class, set2);
        assertThat(set1IsPr).isTrue();
        assertThat(set2IsPr).isFalse();
    }

    private UUID seedUser() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name) "
                        + "VALUES (?, ?, ?, ?)",
                id, id + "@test.local", "x", "Test User");
        return id;
    }

    private UUID seedExercise() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO exercises (id, name_tr, name_en, category, equipment, "
                        + "muscle_primary, difficulty) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                id, "Test TR " + id, "Test EN " + id, "push", "dumbbell",
                "chest", "beginner");
        return id;
    }
}
