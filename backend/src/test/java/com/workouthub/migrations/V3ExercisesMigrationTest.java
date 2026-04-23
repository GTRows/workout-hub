package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class V3ExercisesMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void exercisesTableHasAllExpectedColumnsAfterMigrations() {
        // Checked after full migration chain (V3 + V8) -- covers both the
        // original V3 columns and the bilingual renames/additions from V8.
        var columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'exercises'",
                String.class);
        assertThat(columns).contains(
                "id", "name_tr", "name_en", "category", "equipment",
                "muscle_primary", "muscle_secondary",
                "image_url", "video_url", "difficulty",
                "created_at", "updated_at");
    }

    @Test
    void nameEnIsUnique() {
        Integer unique = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'exercises'
                  AND constraint_type = 'UNIQUE'
                  AND constraint_name = 'exercises_name_en_unique'
                """,
                Integer.class);
        assertThat(unique).isEqualTo(1);
    }

    @Test
    void categoryEquipmentDifficultyCheckConstraintsExist() {
        var checks = jdbc.queryForList(
                """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_name = 'exercises'
                  AND constraint_type = 'CHECK'
                """,
                String.class);
        assertThat(checks).contains(
                "exercises_category_chk",
                "exercises_equipment_chk",
                "exercises_difficulty_chk");
    }

    @Test
    void filterAndSearchIndexesExist() {
        var indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'exercises'",
                String.class);
        assertThat(indexes).contains(
                "idx_exercises_category",
                "idx_exercises_equipment",
                "idx_exercises_difficulty",
                "idx_exercises_name_tr_lower");
    }

}
