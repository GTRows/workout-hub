package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
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

        mvc.perform(get("/api/export/full").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.user.email").value(u.email()))
                .andExpect(jsonPath("$.bodyMetrics.length()").value(1))
                .andExpect(jsonPath("$.supplements.length()").value(1));
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
}
