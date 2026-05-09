package com.workouthub.sessions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SessionHistoryIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "HistSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkoutSessionRepository sessions;

    @Test
    void historyReturnsOnlyCallerOwnSessionsMostRecentFirst() throws Exception {
        SeededUser user = helpers.seed(
                "h-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser other = helpers.seed(
                "o-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        UUID first = startAndFinish(auth);
        UUID second = startAndFinish(auth);
        startAndFinish("Bearer " + other.accessToken());

        mvc.perform(get("/api/sessions/history").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[0].id").value(second.toString()))
                .andExpect(jsonPath("$.content[1].id").value(first.toString()));
    }

    @Test
    void detailReturnsFullSessionIncludingSets() throws Exception {
        SeededUser user = helpers.seed(
                "d-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID sessionId = startAndFinish(auth);

        mvc.perform(get("/api/sessions/" + sessionId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.sets").isArray());
    }

    @Test
    void detailForAnotherUsersSessionReturns404() throws Exception {
        SeededUser owner = helpers.seed(
                "own-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser intruder = helpers.seed(
                "int-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startAndFinish("Bearer " + owner.accessToken());

        mvc.perform(get("/api/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + intruder.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void detailForUnknownSessionReturns404() throws Exception {
        SeededUser user = helpers.seed(
                "unk-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/sessions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyRespectsPageSize() throws Exception {
        SeededUser user = helpers.seed(
                "pg-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        startAndFinish(auth);
        startAndFinish(auth);
        startAndFinish(auth);

        mvc.perform(get("/api/sessions/history?page=0&size=2").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(Matchers.greaterThanOrEqualTo(3)));
    }

    @Test
    void historyRowsExposeHeartRateAvgBpm() throws Exception {
        SeededUser user = helpers.seed(
                "hr-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        UUID firstId = startAndFinish(auth);
        WorkoutSession first = sessions.findById(firstId).orElseThrow();
        first.setHeartRateAvgBpm((short) 138);
        sessions.saveAndFlush(first);

        mvc.perform(get("/api/sessions/history").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$.content[0].heartRateAvgBpm").value(138));

        startAndFinish(auth);

        mvc.perform(get("/api/sessions/history").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].heartRateAvgBpm").doesNotExist());
    }

    private UUID startAndFinish(String auth) throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
        mvc.perform(post("/api/sessions/" + id + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        return id;
    }
}
