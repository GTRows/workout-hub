package com.workouthub.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AlertRulesTest {

    private static final Path RULES_PATH = Path.of(
            System.getProperty("user.dir"),
            "..",
            "observability",
            "alerts",
            "workouthub.rules.yml");

    @Test
    void yamlParsesAndDeclaresTheFourNamedRules() throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(RULES_PATH));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) root.get("groups");
        assertThat(groups).hasSize(1);

        Map<String, Object> group = groups.get(0);
        assertThat(group.get("name")).isEqualTo("workouthub.rules");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules =
                (List<Map<String, Object>>) group.get("rules");
        assertThat(rules).hasSize(4);

        List<String> names = rules.stream()
                .map(r -> (String) r.get("alert"))
                .toList();
        assertThat(names).containsExactlyInAnyOrder(
                "WorkoutHubHigh5xxRate",
                "WorkoutHubDbPoolSaturation",
                "WorkoutHubLongGC",
                "WorkoutHubDiskPressure");
    }

    @Test
    void everyRuleCarriesSeverityLabelAndRunbookAnnotation() throws Exception {
        Map<String, Object> root = new Yaml().load(Files.readString(RULES_PATH));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rules = (List<Map<String, Object>>)
                ((List<Map<String, Object>>) root.get("groups")).get(0).get("rules");

        for (Map<String, Object> rule : rules) {
            @SuppressWarnings("unchecked")
            Map<String, Object> labels = (Map<String, Object>) rule.get("labels");
            @SuppressWarnings("unchecked")
            Map<String, Object> annotations = (Map<String, Object>) rule.get("annotations");
            assertThat((String) labels.get("severity"))
                    .as("severity on %s", rule.get("alert"))
                    .isNotBlank();
            assertThat((String) annotations.get("runbook"))
                    .as("runbook on %s", rule.get("alert"))
                    .startsWith("docs/OBSERVABILITY.md");
        }
    }
}
