package com.workouthub.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GoogleFitParserTest {

    @Test
    void parsesWeightDataPoints() throws Exception {
        String json = """
                {
                  "dataPoint": [
                    {
                      "dataTypeName": "com.google.weight",
                      "startTimeNanos": "1736939400000000000",
                      "endTimeNanos": "1736939400000000000",
                      "fitValue": [{"value": {"fpVal": 80.5}}]
                    },
                    {
                      "dataTypeName": "com.google.step_count.delta",
                      "startTimeNanos": "1736939400000000000",
                      "endTimeNanos": "1736939400000000000",
                      "fitValue": [{"value": {"intVal": 1234}}]
                    }
                  ]
                }
                """;
        ParsedHealth parsed = new GoogleFitParser()
                .parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.bodyMass()).hasSize(1);
        assertThat(parsed.bodyMass().get(0).kg())
                .isEqualByComparingTo(new BigDecimal("80.50"));
        assertThat(parsed.workouts()).isEmpty();
    }

    @Test
    void parsesSessionEnvelope() throws Exception {
        String json = """
                {
                  "session": [
                    {
                      "id": "1736953200000",
                      "name": "Strength training",
                      "startTimeMillis": "1736953200000",
                      "endTimeMillis": "1736956800000",
                      "activityType": 80
                    }
                  ]
                }
                """;
        ParsedHealth parsed = new GoogleFitParser()
                .parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.workouts()).hasSize(1);
        ParsedHealth.WorkoutRecord w = parsed.workouts().get(0);
        assertThat(w.notes()).contains("Strength training");
        assertThat(w.startedAt().toEpochMilli()).isEqualTo(1736953200000L);
        assertThat(w.endedAt().toEpochMilli()).isEqualTo(1736956800000L);
    }

    @Test
    void parsesArrayOfSessions() throws Exception {
        String json = """
                [
                  {
                    "id": "1",
                    "name": "Run",
                    "startTimeMillis": "1736953200000",
                    "endTimeMillis": "1736956800000",
                    "activityType": 8
                  }
                ]
                """;
        ParsedHealth parsed = new GoogleFitParser()
                .parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertThat(parsed.workouts()).hasSize(1);
        assertThat(parsed.workouts().get(0).activityType()).isEqualTo("google.fit.8");
    }
}
