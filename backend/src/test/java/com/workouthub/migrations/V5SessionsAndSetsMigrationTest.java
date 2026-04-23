package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workouthub.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class V5SessionsAndSetsMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void bothTablesExist() {
        var tables = jdbc.queryForList(
                """
                SELECT table_name FROM information_schema.tables
                WHERE table_name IN ('workout_sessions', 'session_sets')
                """,
                String.class);
        assertThat(tables).containsExactlyInAnyOrder("workout_sessions", "session_sets");
    }

    @Test
    void sessionIdForeignKeyCascades() {
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

        jdbc.update("DELETE FROM workout_sessions WHERE id = ?", sessionId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM session_sets WHERE id = ?",
                Integer.class, setId);
        assertThat(remaining).isZero();
    }

    @Test
    void setNumberUniquenessIsPerSessionAndExercise() {
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

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), sessionId, exerciseId, 1, 8))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rpeOutsideOneToTenIsRejected() {
        UUID userId = seedUser();
        UUID sessionId = UUID.randomUUID();
        UUID exerciseId = seedExercise();
        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id) VALUES (?, ?)",
                sessionId, userId);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO session_sets "
                        + "(id, session_id, exercise_id, set_number, reps_done, rpe) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), sessionId, exerciseId, 1, 8, 11))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void atMostOneActiveSessionPerUser() {
        UUID userId = seedUser();
        jdbc.update(
                "INSERT INTO workout_sessions (id, user_id) VALUES (?, ?)",
                UUID.randomUUID(), userId);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO workout_sessions (id, user_id) VALUES (?, ?)",
                UUID.randomUUID(), userId))
                .isInstanceOf(DataIntegrityViolationException.class);
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
