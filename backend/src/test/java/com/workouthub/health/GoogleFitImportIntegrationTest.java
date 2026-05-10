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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class GoogleFitImportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "GoogleSecret1!";

    private static final String FIXTURE = """
            {
              "dataPoint": [
                {
                  "dataTypeName": "com.google.weight",
                  "startTimeNanos": "1736939400000000000",
                  "endTimeNanos": "1736939400000000000",
                  "fitValue": [{"value": {"fpVal": 80.5}}]
                }
              ],
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

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void importsWeightAndSessionEndToEnd() throws Exception {
        SeededUser u = helpers.seed(
                "gf-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        MockMultipartFile file = new MockMultipartFile(
                "file", "fitness.json", "application/json",
                FIXTURE.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/health/import/google-fit")
                        .file(file)
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyMassImported").value(1))
                .andExpect(jsonPath("$.workoutsImported").value(1));
    }
}
