package com.workouthub.metrics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class MetricsIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "MetricsSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void unauthenticatedCallReturns401() throws Exception {
        mvc.perform(get("/api/metrics")).andExpect(status().isUnauthorized());
    }

    @Test
    void postStoresAndGetReturnsTheEntry() throws Exception {
        SeededUser user = helpers.seed(
                "m-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordedDate":"2026-04-20","weightKg":78.5,"notes":"morning"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg").value(78.5));

        mvc.perform(get("/api/metrics").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordedDate").value("2026-04-20"))
                .andExpect(jsonPath("$[0].notes").value("morning"));
    }

    @Test
    void repeatedPostForSameDateUpdatesInsteadOfInserting() throws Exception {
        SeededUser user = helpers.seed(
                "up-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-21\",\"weightKg\":78.0}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-21\",\"weightKg\":77.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightKg").value(77.5));

        mvc.perform(get("/api/metrics").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void invalidWeightRangeReturns400() throws Exception {
        SeededUser user = helpers.seed(
                "bad-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/metrics")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-22\",\"weightKg\":5.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteRemovesTheEntry() throws Exception {
        SeededUser user = helpers.seed(
                "d-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        MvcResult created = mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-23\",\"weightKg\":78}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(delete("/api/metrics/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void crossUserListIsIsolated() throws Exception {
        SeededUser a = helpers.seed(
                "a-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser b = helpers.seed(
                "b-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/metrics")
                        .header("Authorization", "Bearer " + a.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-04-24\",\"weightKg\":80}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/metrics")
                        .header("Authorization", "Bearer " + b.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void postWithPhotoUrlIsRoundTripped() throws Exception {
        SeededUser user = helpers.seed(
                "photo-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordedDate":"2026-04-25","weightKg":78.0,"photoUrl":"https://example.test/photos/abc.jpg"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrl").value("https://example.test/photos/abc.jpg"));

        mvc.perform(get("/api/metrics").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].photoUrl").value("https://example.test/photos/abc.jpg"));
    }

    @Test
    void rangeFilterReturnsOnlyMatchingRows() throws Exception {
        SeededUser user = helpers.seed(
                "range-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        seedMetric(auth, "2026-03-01", 78.0);
        seedMetric(auth, "2026-03-15", 78.5);
        seedMetric(auth, "2026-03-31", 79.0);

        mvc.perform(get("/api/metrics?from=2026-03-10&to=2026-03-20")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].recordedDate").value("2026-03-15"));
    }

    @Test
    void rangeFilterIsInclusiveOnBothBounds() throws Exception {
        SeededUser user = helpers.seed(
                "incl-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        seedMetric(auth, "2026-04-01", 80.0);
        seedMetric(auth, "2026-04-30", 79.5);

        mvc.perform(get("/api/metrics?from=2026-04-01&to=2026-04-30")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void rangeWithNoMatchingRowsReturnsEmptyArray() throws Exception {
        SeededUser user = helpers.seed(
                "empty-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        seedMetric(auth, "2026-05-01", 78.0);

        mvc.perform(get("/api/metrics?from=2026-06-01&to=2026-06-30")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rangeMixedOrInvertedReturns400() throws Exception {
        SeededUser user = helpers.seed(
                "mix-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        mvc.perform(get("/api/metrics?from=2026-03-01")
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/metrics?to=2026-03-31")
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/metrics?from=2026-03-31&to=2026-03-01")
                        .header("Authorization", auth))
                .andExpect(status().isBadRequest());
    }

    private void seedMetric(String auth, String recordedDate, double weightKg) throws Exception {
        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"" + recordedDate + "\",\"weightKg\":" + weightKg + "}"))
                .andExpect(status().isCreated());
    }
}
