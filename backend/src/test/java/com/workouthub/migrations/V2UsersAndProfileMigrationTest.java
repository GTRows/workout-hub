package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class V2UsersAndProfileMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void usersTableHasExpectedColumnsInOrder() {
        var columns = jdbc.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = 'users'
                ORDER BY ordinal_position
                """,
                String.class);
        assertThat(columns).containsExactly(
                "id", "email", "password_hash", "display_name", "role",
                "created_at", "updated_at");
    }

    @Test
    void usersEmailUniqueConstraintExists() {
        Integer uniqueConstraints = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'users'
                  AND constraint_type = 'UNIQUE'
                  AND constraint_name = 'users_email_unique'
                """,
                Integer.class);
        assertThat(uniqueConstraints).isEqualTo(1);
    }

    @Test
    void usersRoleCheckConstraintExists() {
        Integer checks = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'users'
                  AND constraint_type = 'CHECK'
                  AND constraint_name = 'users_role_chk'
                """,
                Integer.class);
        assertThat(checks).isEqualTo(1);
    }

    @Test
    void caseInsensitiveEmailIndexExists() {
        var indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'users'",
                String.class);
        assertThat(indexes).contains("idx_users_email_lower");
    }

    @Test
    void userProfileHasForeignKeyToUsers() {
        Integer fkCount = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE table_name = 'user_profile'
                  AND constraint_type = 'FOREIGN KEY'
                """,
                Integer.class);
        assertThat(fkCount).isEqualTo(1);
    }

    @Test
    void userProfileColumnsMatchSchema() {
        var columns = jdbc.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_name = 'user_profile'
                ORDER BY ordinal_position
                """,
                String.class);
        // Later migrations append columns to user_profile (nutrition goals,
        // streak freeze, theme); assert only that the V2-introduced set is present.
        assertThat(columns).contains(
                "user_id", "height_cm", "weight_kg", "birth_date", "gender",
                "health_notes", "goals", "created_at", "updated_at");
    }
}
