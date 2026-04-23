package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.workouthub.support.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class V7RefreshTokensMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void tableExists() {
        Boolean exists = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM information_schema.tables "
                        + "WHERE table_name = 'refresh_tokens')",
                Boolean.class);
        assertThat(exists).isTrue();
    }

    @Test
    void tokenHashIsUnique() {
        UUID userId = seedUser();
        jdbc.update(
                "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, "abc", java.sql.Timestamp.from(Instant.now().plusSeconds(60)));

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), userId, "abc", java.sql.Timestamp.from(Instant.now().plusSeconds(60))))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void cascadesOnUserDelete() {
        UUID userId = seedUser();
        UUID rtId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) "
                        + "VALUES (?, ?, ?, ?)",
                rtId, userId, "cascade-test-" + rtId,
                java.sql.Timestamp.from(Instant.now().plusSeconds(60)));

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE id = ?",
                Integer.class, rtId);
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
