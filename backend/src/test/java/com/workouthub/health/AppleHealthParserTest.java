package com.workouthub.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.health.AppleHealthParser.ParsedHealth;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class AppleHealthParserTest {

    private static final String FIXTURE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <HealthData locale="en_US">
              <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Health" unit="kg"
                      startDate="2026-01-15 08:30:00 +0000" endDate="2026-01-15 08:30:00 +0000"
                      value="80.5"/>
              <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Health" unit="kg"
                      startDate="2026-01-16 08:30:00 +0000" endDate="2026-01-16 08:30:00 +0000"
                      value="80.2"/>
              <Record type="HKQuantityTypeIdentifierStepCount" sourceName="iPhone" unit="count"
                      startDate="2026-01-15 08:30:00 +0000" endDate="2026-01-15 08:30:00 +0000"
                      value="1234"/>
              <Workout workoutActivityType="HKWorkoutActivityTypeTraditionalStrengthTraining"
                       duration="60" durationUnit="min" totalEnergyBurned="350"
                       sourceName="Workouts"
                       startDate="2026-01-15 17:00:00 +0000"
                       endDate="2026-01-15 18:00:00 +0000"/>
            </HealthData>
            """;

    @Test
    void parsesBodyMassRecordsAndIgnoresOtherTypes() throws Exception {
        ParsedHealth parsed = new AppleHealthParser()
                .parse(new ByteArrayInputStream(FIXTURE.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.bodyMass()).hasSize(2);
        assertThat(parsed.bodyMass().get(0).kg()).isEqualByComparingTo(new BigDecimal("80.5"));
        assertThat(parsed.bodyMass().get(0).date().toString()).isEqualTo("2026-01-15");
    }

    @Test
    void parsesWorkoutRecordsWithStartAndEnd() throws Exception {
        ParsedHealth parsed = new AppleHealthParser()
                .parse(new ByteArrayInputStream(FIXTURE.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.workouts()).hasSize(1);
        AppleHealthParser.WorkoutRecord w = parsed.workouts().get(0);
        assertThat(w.activityType()).contains("TraditionalStrengthTraining");
        assertThat(w.startedAt().toString()).isEqualTo("2026-01-15T17:00:00Z");
        assertThat(w.endedAt().toString()).isEqualTo("2026-01-15T18:00:00Z");
        assertThat(w.notes()).contains("Apple Health");
    }

    @Test
    void unwrapsZipContainingExportXml() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            zip.putNextEntry(new ZipEntry("apple_health_export/export.xml"));
            zip.write(FIXTURE.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        ParsedHealth parsed = new AppleHealthParser()
                .parse(new ByteArrayInputStream(baos.toByteArray()));

        assertThat(parsed.bodyMass()).hasSize(2);
        assertThat(parsed.workouts()).hasSize(1);
    }

    @Test
    void convertsLbToKg() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <HealthData>
                  <Record type="HKQuantityTypeIdentifierBodyMass" unit="lb"
                          startDate="2026-01-15 08:30:00 +0000" endDate="2026-01-15 08:30:00 +0000"
                          value="200"/>
                </HealthData>
                """;
        ParsedHealth parsed = new AppleHealthParser()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.bodyMass()).hasSize(1);
        assertThat(parsed.bodyMass().get(0).kg())
                .isEqualByComparingTo(new BigDecimal("90.72"));
    }
}
