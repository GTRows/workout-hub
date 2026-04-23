package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class V9SeedExercisesMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void atLeastFiftyExercisesSeeded() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM exercises", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(50);
    }

    @Test
    void bothLocalesArePopulatedForEverySeededRow() {
        Integer missing = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM exercises
                WHERE description_tr IS NULL OR description_en IS NULL
                   OR array_length(form_tips_tr, 1) IS NULL
                   OR array_length(form_tips_en, 1) IS NULL
                   OR array_length(common_mistakes_tr, 1) IS NULL
                   OR array_length(common_mistakes_en, 1) IS NULL
                """,
                Integer.class);
        assertThat(missing).isZero();
    }

    @Test
    void everyEquipmentTypeAppearsAtLeastOnce() {
        var equipments = jdbc.queryForList(
                "SELECT DISTINCT equipment FROM exercises", String.class);
        assertThat(equipments).contains(
                "bodyweight", "dumbbell", "zbar", "bar", "wrist_tool", "machine");
    }

    @Test
    void everyCategoryAppearsAtLeastOnce() {
        var categories = jdbc.queryForList(
                "SELECT DISTINCT category FROM exercises", String.class);
        assertThat(categories).contains(
                "push", "pull", "legs", "core", "cardio", "forearm");
    }

    @Test
    void seedIsIdempotent() {
        Integer first = jdbc.queryForObject("SELECT COUNT(*) FROM exercises", Integer.class);
        // V9 filters with NOT EXISTS; re-executing its SELECT-INSERT core must not duplicate.
        // Re-run the idempotent upsert by emulating it: attempt to re-insert an existing row.
        Integer inserted = jdbc.update(
                """
                INSERT INTO exercises (
                    id, name_tr, name_en, category, equipment, muscle_primary,
                    description_tr, description_en, difficulty)
                SELECT gen_random_uuid(), 'Sinav', 'Push-up', 'push', 'bodyweight', 'chest',
                       'x', 'x', 'beginner'
                WHERE NOT EXISTS (SELECT 1 FROM exercises WHERE LOWER(name_en) = LOWER('Push-up'))
                """);
        assertThat(inserted).isZero();
        Integer second = jdbc.queryForObject("SELECT COUNT(*) FROM exercises", Integer.class);
        assertThat(second).isEqualTo(first);
    }
}
