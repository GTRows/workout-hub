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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SectionExportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "SectionSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void unauthenticatedSliceReturns401() throws Exception {
        mvc.perform(get("/api/export/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void profileSliceReturnsUserSectionShape() throws Exception {
        SeededUser u = helpers.seed(
                "sec-prof-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/export/profile")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(u.email()))
                .andExpect(jsonPath("$.displayName").exists());
    }

    @Test
    void metricsSliceMatchesFullExportMetricsSlice() throws Exception {
        SeededUser u = helpers.seed(
                "sec-m-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-22\",\"weightKg\":80.0}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/export/metrics").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordedDate").value("2026-04-22"))
                .andExpect(jsonPath("$[0].weightKg").value(80.0));
    }

    @Test
    void supplementsSliceReturnsArrayShape() throws Exception {
        SeededUser u = helpers.seed(
                "sec-s-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Creatine\",\"timing\":\"morning\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/export/supplements").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Creatine"))
                .andExpect(jsonPath("$[0].timing").value("morning"));
    }

    @Test
    void plansSliceReturnsSeededDefaultPlan() throws Exception {
        SeededUser u = helpers.seed(
                "sec-p-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/export/plans")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void sessionsSliceReturnsEmptyArrayForFreshUser() throws Exception {
        SeededUser u = helpers.seed(
                "sec-ss-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/export/sessions")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
