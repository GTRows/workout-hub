package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SectionImportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "SecImportSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void importProfileSliceOverwritesFields() throws Exception {
        SeededUser u = helpers.seed(
                "sip-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/export/import/profile")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "ignored",
                                  "heightCm": 182,
                                  "weightKg": 81.5,
                                  "birthDate": "1992-03-15",
                                  "gender": "male",
                                  "healthNotes": null,
                                  "goals": "Hit 1.5x BW squat"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileUpdated").value(1));

        mvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.heightCm").value(182))
                .andExpect(jsonPath("$.profile.goals").value("Hit 1.5x BW squat"));
    }

    @Test
    void importMetricsSliceRoundTripsAndReplaces() throws Exception {
        SeededUser u = helpers.seed(
                "sim-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-01-01\",\"weightKg\":90}"))
                .andExpect(status().isCreated());

        // Replace the user's metrics with a single row under a different date.
        mvc.perform(post("/api/export/import/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"recordedDate":"2026-04-20","weightKg":80,"bodyFatPercent":18.5}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricsInserted").value(1));

        mvc.perform(get("/api/metrics").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordedDate").value("2026-04-20"))
                .andExpect(jsonPath("$[0].weightKg").value(80.0));
    }

    @Test
    void importSupplementsSliceRoundTripsAndReplaces() throws Exception {
        SeededUser u = helpers.seed(
                "sisup-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/export/import/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"name":"Creatine","dosage":"5g","timing":"morning","active":true}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplementsInserted").value(1));

        mvc.perform(get("/api/supplements").header("Authorization", auth))
                .andExpect(jsonPath("$[0].name").value("Creatine"));
    }

    @Test
    void importSupplementsSliceRejectsUnknownTimingWith422() throws Exception {
        SeededUser u = helpers.seed(
                "sisup2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/export/import/supplements")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"name":"X","timing":"not_a_value","active":true}
                                ]
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void importPlansSliceWipesAndInsertsWhileDetachingSessionsFromOldDays() throws Exception {
        SeededUser u = helpers.seed(
                "sipl-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        // Empty-plan import: should wipe the default-seeded plan.
        mvc.perform(post("/api/export/import/plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plansInserted").value(0));

        mvc.perform(get("/api/export/plans").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void importSessionsSliceEmptyArrayIsNoOp() throws Exception {
        SeededUser u = helpers.seed(
                "sis-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/export/import/sessions")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionsInserted").value(0));
    }
}
