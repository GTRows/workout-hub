package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workouthub.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class V26SessionSetsClientIdMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void clientSetIdColumnExists() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                """
                SELECT udt_name, is_nullable
                FROM information_schema.columns
                WHERE table_name = 'session_sets'
                  AND column_name = 'client_set_id'
                """);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("udt_name")).isEqualTo("uuid");
        assertThat(rows.get(0).get("is_nullable")).isEqualTo("YES");
    }

    @Test
    void partialUniqueIndexRejectsDuplicateKey() {
        UUID userId = seedUser();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = seedExercise();
        UUID clientKey = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id) VALUES (?, ?)",
                sessionId, userId);
        jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done, client_set_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), sessionId, exerciseId, 1, 8, clientKey);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done, client_set_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), sessionId, exerciseId, 2, 10, clientKey))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void partialUniqueIndexAllowsManyNulls() {
        UUID userId = seedUser();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = seedExercise();

        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id) VALUES (?, ?)",
                sessionId, userId);
        jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), sessionId, exerciseId, 1, 8);
        jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), sessionId, exerciseId, 2, 10);

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM session_sets WHERE session_id = ? AND client_set_id IS NULL",
                Integer.class, sessionId);
        assertThat(count).isEqualTo(2);
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
