package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workouthub.support.AbstractIntegrationTest;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class V6BodyMetricsAndSupplementsMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void bothTablesExist() {
        var tables = jdbc.queryForList(
                """
                SELECT table_name FROM information_schema.tables
                WHERE table_name IN ('body_metrics', 'supplements')
                """,
                String.class);
        assertThat(tables).containsExactlyInAnyOrder("body_metrics", "supplements");
    }

    @Test
    void bodyMetricsHasDateIndex() {
        var indexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'body_metrics'",
                String.class);
        assertThat(indexes).contains(
                "idx_body_metrics_recorded_date",
                "idx_body_metrics_user_date");
    }

    @Test
    void duplicateMetricOnSameDayIsRejected() {
        UUID userId = seedUser();
        LocalDate today = LocalDate.now();
        jdbc.update(
                "INSERT INTO body_metrics (id, user_id, recorded_date, weight_kg) "
                        + "VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, today, 78.0);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO body_metrics (id, user_id, recorded_date, weight_kg) "
                        + "VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, today, 78.5))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bodyFatOutsideZeroToOneHundredIsRejected() {
        UUID userId = seedUser();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO body_metrics (id, user_id, recorded_date, body_fat_percent) "
                        + "VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, LocalDate.now(), 150.0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void supplementsTimingCheckEnforced() {
        UUID userId = seedUser();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO supplements (id, user_id, name, timing) "
                        + "VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, "Creatine", "while_sleeping"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void metricsCascadeOnUserDelete() {
        UUID userId = seedUser();
        UUID metricId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO body_metrics (id, user_id, recorded_date, weight_kg) "
                        + "VALUES (?, ?, ?, ?)",
                metricId, userId, LocalDate.now(), 78.0);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM body_metrics WHERE id = ?",
                Integer.class, metricId);
        assertThat(remaining).isZero();
    }

    private UUID seedUser() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name) "
                        + "VALUES (?, ?, ?, ?)",
                id, id + "@test.local", "x", "Test User");
        return id;
    }
}
