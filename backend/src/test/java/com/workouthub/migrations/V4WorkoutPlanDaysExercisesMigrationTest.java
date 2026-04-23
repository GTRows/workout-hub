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
class V4WorkoutPlanDaysExercisesMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allThreeTablesExist() {
        var tables = jdbc.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_name IN ('workout_plans', 'workout_days', 'workout_day_exercises')
                """,
                String.class);
        assertThat(tables).containsExactlyInAnyOrder(
                "workout_plans", "workout_days", "workout_day_exercises");
    }

    @Test
    void workoutDaysHasUniquePlanAndDayOfWeek() {
        Integer count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'workout_days'
                  AND constraint_name = 'workout_days_plan_day_unique'
                  AND constraint_type = 'UNIQUE'
                """,
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void workoutPlansCascadesOnUserDelete() {
        UUID userId = seedUser();
        UUID planId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO workout_plans (id, user_id, name) VALUES (?, ?, ?)",
                planId, userId, "Cascade Test");

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workout_plans WHERE id = ?",
                Integer.class, planId);
        assertThat(remaining).isZero();
    }

    @Test
    void workoutDaysCascadeOnPlanDelete() {
        UUID userId = seedUser();
        UUID planId = UUID.randomUUID();
        UUID dayId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO workout_plans (id, user_id, name) VALUES (?, ?, ?)",
                planId, userId, "Cascade Test");
        jdbc.update(
                "INSERT INTO workout_days (id, plan_id, day_of_week, name, focus) "
                        + "VALUES (?, ?, ?, ?, ?)",
                dayId, planId, 1, "Monday", "push");

        jdbc.update("DELETE FROM workout_plans WHERE id = ?", planId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM workout_days WHERE id = ?",
                Integer.class, dayId);
        assertThat(remaining).isZero();
    }

    @Test
    void dayOfWeekOutsideOneToSevenIsRejected() {
        UUID userId = seedUser();
        UUID planId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO workout_plans (id, user_id, name) VALUES (?, ?, ?)",
                planId, userId, "Invalid Day Test");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO workout_days (id, plan_id, day_of_week, name, focus) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), planId, 8, "Bad", "push"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void exerciseInUseCannotBeDeleted() {
        UUID userId = seedUser();
        UUID exerciseId = seedExercise();
        UUID planId = UUID.randomUUID();
        UUID dayId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO workout_plans (id, user_id, name) VALUES (?, ?, ?)",
                planId, userId, "In-use Test");
        jdbc.update(
                "INSERT INTO workout_days (id, plan_id, day_of_week, name, focus) "
                        + "VALUES (?, ?, ?, ?, ?)",
                dayId, planId, 1, "Monday", "push");
        jdbc.update(
                "INSERT INTO workout_day_exercises "
                        + "(id, workout_day_id, exercise_id, order_index, target_sets) "
                        + "VALUES (?, ?, ?, ?, ?)",
                UUID.randomUUID(), dayId, exerciseId, 1, 3);

        assertThatThrownBy(() ->
                jdbc.update("DELETE FROM exercises WHERE id = ?", exerciseId))
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
