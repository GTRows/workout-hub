package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class FullExportImportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "ExportSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    @Test
    void unauthenticatedFullExportReturns401() throws Exception {
        mvc.perform(get("/api/export/full")).andExpect(status().isUnauthorized());
    }

    @Test
    void fullExportReturnsSchemaVersionAndCurrentUserSlices() throws Exception {
        SeededUser u = helpers.seed(
                "fe1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        // Seed one metric and one supplement so the export is non-empty.
        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-20\",\"weightKg\":78.0}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Creatine\",\"timing\":\"morning\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/workout-plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Export Seed\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/export/full").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.user.email").value(u.email()))
                .andExpect(jsonPath("$.bodyMetrics.length()").value(1))
                .andExpect(jsonPath("$.supplements.length()").value(1))
                .andExpect(jsonPath("$.plans.length()").value(1));
    }

    @Test
    void importRejectsUnsupportedSchemaVersion() throws Exception {
        SeededUser u = helpers.seed(
                "fe2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/export/import")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schemaVersion": 99,
                                  "exportedAt": "2026-04-23T00:00:00Z",
                                  "user": null,
                                  "plans": [],
                                  "sessions": [],
                                  "bodyMetrics": [],
                                  "supplements": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void importRejectsPayloadsWithMultipleActivePlans() throws Exception {
        SeededUser u = helpers.seed(
                "fe3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/export/import")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "schemaVersion": 1,
                                  "exportedAt": "2026-04-23T00:00:00Z",
                                  "user": null,
                                  "plans": [
                                    {"id":"ffffffff-1111-1111-1111-111111111111","name":"A","active":true,"days":[]},
                                    {"id":"ffffffff-2222-2222-2222-222222222222","name":"B","active":true,"days":[]}
                                  ],
                                  "sessions": [],
                                  "bodyMetrics": [],
                                  "supplements": []
                                }
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void importRoundTripPreservesPlansFromExport() throws Exception {
        SeededUser u = helpers.seed(
                "rt-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        // TestAuthHelpers.seed bypasses DefaultPlanSeeder, so create a plan explicitly before exporting.
        mvc.perform(post("/api/workout-plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Round-Trip Seed\"}"))
                .andExpect(status().isCreated());

        MvcResult exp = mvc.perform(get("/api/export/full").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        String dump = exp.getResponse().getContentAsString();

        mvc.perform(post("/api/export/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dump))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plansInserted").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mvc.perform(get("/api/workout-plans/active").header("Authorization", auth))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void importRoundTripReplacesMetricsAndSupplements() throws Exception {
        SeededUser u = helpers.seed(
                "fe4-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        // Seed some initial metrics and supplements.
        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-03-01\",\"weightKg\":80.0}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Omega-3\",\"timing\":\"with_meal\"}"))
                .andExpect(status().isCreated());

        // Export.
        MvcResult exp = mvc.perform(get("/api/export/full").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        String dump = exp.getResponse().getContentAsString();

        // Replace with just ONE metric and ZERO supplements in the payload,
        // then import it back. The server should reflect the new state.
        String modified = dump
                .replace(
                        "\"supplements\":[",
                        "\"supplements\":[")
                .replaceFirst(
                        "\"supplements\":\\[[^]]*]",
                        "\"supplements\":[]");

        mvc.perform(post("/api/export/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modified))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileUpdated").value(1))
                .andExpect(jsonPath("$.supplementsInserted").value(0));

        mvc.perform(get("/api/supplements").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/metrics").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void prFlagSurvivesFullExportImportRoundTrip() throws Exception {
        SeededUser u = helpers.seed(
                "rt-pr-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        UUID exerciseId = seedExercise();

        // Start a session, add a single PR set (first ever set always flags PR), finish it.
        MvcResult start = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionId = objectMapper.readTree(start.getResponse().getContentAsString())
                .get("id").asText();

        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":1,\"repsDone\":5,\"weightKg\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newPr").value(true));

        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Export.
        MvcResult exp = mvc.perform(get("/api/export/full").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        String dump = exp.getResponse().getContentAsString();

        // Re-import the same payload (wipes-then-reinserts).
        mvc.perform(post("/api/export/import")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dump))
                .andExpect(status().isOk());

        // Look up the (only) re-imported finished session via /api/sessions/history,
        // then GET its detail to assert the PR flag survived the round-trip.
        MvcResult list = mvc.perform(get("/api/sessions/history").header("Authorization", auth))
                .andExpect(status().isOk())
                .andReturn();
        String reimportedSessionId = objectMapper.readTree(list.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText();

        mvc.perform(get("/api/sessions/" + reimportedSessionId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets[0].newPr").value(true));
    }

    @Test
    void importRejectsSessionWithUnknownWorkoutDayId() throws Exception {
        SeededUser u = helpers.seed(
                "fe-staleday-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        // Hand-crafted payload: plans is empty, but a session references a
        // non-existent workoutDayId. Validator must surface this as 422,
        // not let it crash insertSessions with an FK violation (500-class).
        String stalePayload = """
                {
                  "schemaVersion": 1,
                  "exportedAt": "2026-04-23T00:00:00Z",
                  "user": null,
                  "plans": [],
                  "sessions": [
                    {
                      "id": "11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                      "workoutDayId": "22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                      "startedAt": "2026-04-23T09:00:00Z",
                      "endedAt": "2026-04-23T10:00:00Z",
                      "notes": null,
                      "mood": null,
                      "energyLevel": null,
                      "sets": []
                    }
                  ],
                  "bodyMetrics": [],
                  "supplements": []
                }
                """;

        mvc.perform(post("/api/export/import")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stalePayload))
                .andExpect(status().isUnprocessableEntity());
    }

    private UUID seedExercise() {
        String tag = "fe-pr-" + System.nanoTime();
        Exercise e = new Exercise();
        e.setNameTr("TR " + tag);
        e.setNameEn("EN " + tag);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.DUMBBELL);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.INTERMEDIATE);
        return exerciseRepo.save(e).getId();
    }
}
