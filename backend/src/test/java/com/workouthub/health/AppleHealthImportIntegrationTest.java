package com.workouthub.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AppleHealthImportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "AppleSecret1!";

    private static final String FIXTURE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <HealthData locale="en_US">
              <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Health" unit="kg"
                      startDate="2026-01-15 08:30:00 +0000" endDate="2026-01-15 08:30:00 +0000"
                      value="80.5"/>
              <Record type="HKQuantityTypeIdentifierBodyMass" sourceName="Health" unit="kg"
                      startDate="2026-01-16 08:30:00 +0000" endDate="2026-01-16 08:30:00 +0000"
                      value="80.2"/>
              <Workout workoutActivityType="HKWorkoutActivityTypeTraditionalStrengthTraining"
                       duration="60" durationUnit="min" sourceName="Workouts"
                       startDate="2026-01-15 17:00:00 +0000"
                       endDate="2026-01-15 18:00:00 +0000"/>
            </HealthData>
            """;

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void importsBodyMassAndWorkoutEndToEnd() throws Exception {
        SeededUser u = helpers.seed(
                "ah-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "export.xml", "application/xml",
                FIXTURE.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/health/import/apple")
                        .file(file)
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMassImported").value(2))
                .andExpect(jsonPath("$.workoutsImported").value(1));
    }

    @Test
    void skipsRecordsThatAlreadyExist() throws Exception {
        SeededUser u = helpers.seed(
                "ah2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "export.xml", "application/xml",
                FIXTURE.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/health/import/apple")
                        .file(file)
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk());

        MockMultipartFile file2 = new MockMultipartFile(
                "file", "export.xml", "application/xml",
                FIXTURE.getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/health/import/apple")
                        .file(file2)
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMassImported").value(0))
                .andExpect(jsonPath("$.bodyMassSkipped").value(2))
                .andExpect(jsonPath("$.workoutsImported").value(0))
                .andExpect(jsonPath("$.workoutsSkipped").value(1));
    }

    @Test
    void respectsBodyMassToggle() throws Exception {
        SeededUser u = helpers.seed(
                "ah3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "export.xml", "application/xml",
                FIXTURE.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/health/import/apple")
                        .file(file)
                        .param("bodyMass", "false")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMassImported").value(0))
                .andExpect(jsonPath("$.workoutsImported").value(1));
    }
}
