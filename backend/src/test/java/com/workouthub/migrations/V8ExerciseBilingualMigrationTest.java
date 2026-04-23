package com.workouthub.migrations;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.support.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class V8ExerciseBilingualMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void renamedAndAddedColumnsArePresent() {
        var columns = jdbc.queryForList(
                """
                SELECT column_name FROM information_schema.columns
                WHERE table_name = 'exercises'
                """,
                String.class);
        assertThat(columns).contains(
                "description_tr", "description_en",
                "form_tips_tr", "form_tips_en",
                "common_mistakes_tr", "common_mistakes_en");
        assertThat(columns).doesNotContain("form_tips", "common_mistakes");
    }

    @Test
    void englishArrayColumnsAreTextArrays() {
        var rows = jdbc.queryForList(
                """
                SELECT column_name, udt_name
                FROM information_schema.columns
                WHERE table_name = 'exercises'
                  AND column_name IN ('form_tips_en', 'common_mistakes_en',
                                      'form_tips_tr', 'common_mistakes_tr')
                """,
                Map.class);
        assertThat(rows).hasSize(4);
        assertThat(rows).allMatch(r -> "_text".equals(r.get("udt_name")),
                "expected all four array columns to use the PostgreSQL text[] (_text) udt");
    }
}
