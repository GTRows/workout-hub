package com.workouthub.observability;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Guards observability/grafana/workouthub.json - the dashboard that gets
 * imported into the homelab Grafana. Tests catch accidental drift that
 * would make the dashboard unusable before it hits the server.
 */
class GrafanaDashboardTest {

    private static final Path DASHBOARD_PATH = Path.of(
            System.getProperty("user.dir"),
            "..",
            "observability",
            "grafana",
            "workouthub.json");

    @Test
    void dashboardIsValidJsonWithExpectedTopLevelKeys() throws Exception {
        JsonNode root = new ObjectMapper().readTree(DASHBOARD_PATH.toFile());
        assertThat(root.get("title").asText()).isEqualTo("WorkoutHub");
        assertThat(root.get("uid").asText()).isEqualTo("workouthub-overview");
        assertThat(root.has("panels")).isTrue();
    }

    @Test
    void dashboardHasSixPanelsCoveringTheRequiredAcceptanceCriteria() throws Exception {
        JsonNode panels = new ObjectMapper().readTree(DASHBOARD_PATH.toFile()).get("panels");
        assertThat(panels.isArray()).isTrue();
        assertThat(panels.size()).isEqualTo(6);

        String titles = panels.toString();
        assertThat(titles).contains("Request rate");
        assertThat(titles).contains("HTTP 5xx");
        assertThat(titles).contains("p95 latency");
        assertThat(titles).contains("HikariCP");
        assertThat(titles).contains("JVM heap");
    }
}
