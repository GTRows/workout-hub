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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SessionLifecycleIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "SessSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkoutSessionRepository sessions;

    @Test
    void unauthenticatedStartReturns401() throws Exception {
        mvc.perform(post("/api/sessions/start").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void startActiveFinishFlow() throws Exception {
        SeededUser user = helpers.seed(
                "s-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        // No active session at first: 204.
        mvc.perform(get("/api/sessions/active").header("Authorization", auth))
                .andExpect(status().isNoContent());

        MvcResult start = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finished").value(false))
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper.readTree(
                start.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/sessions/active").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()));

        // Second start while one is active -> 409.
        mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ACTIVE"));

        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"good day\",\"mood\":4,\"energyLevel\":4}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finished").value(true))
                .andExpect(jsonPath("$.mood").value(4))
                .andExpect(jsonPath("$.notes").value("good day"));

        // After finish, active is gone again.
        mvc.perform(get("/api/sessions/active").header("Authorization", auth))
                .andExpect(status().isNoContent());

        // Finishing a finished session is 409.
        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_FINISHED"));
    }

    @Test
    void startWithUnknownDayIdReturns404() throws Exception {
        SeededUser user = helpers.seed(
                "unk-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/sessions/start")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workoutDayId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void finishingOtherUsersSessionReturns404() throws Exception {
        SeededUser owner = helpers.seed(
                "o-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser intruder = helpers.seed(
                "i-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        MvcResult r = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", "Bearer " + intruder.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void moodOutOfRangeOnFinishReturns400() throws Exception {
        SeededUser user = helpers.seed(
                "mood-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        MvcResult r = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mood\":9}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void heartRateAvgBpmIsNullByDefaultAndPopulatedAfterImporterWrite() throws Exception {
        SeededUser user = helpers.seed(
                "hr-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        MvcResult start = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper.readTree(
                start.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/sessions/" + sessionId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heartRateAvgBpm").doesNotExist());

        WorkoutSession s = sessions.findById(sessionId).orElseThrow();
        s.setHeartRateAvgBpm((short) 142);
        sessions.saveAndFlush(s);

        mvc.perform(get("/api/sessions/" + sessionId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heartRateAvgBpm").value(142));

        mvc.perform(get("/api/sessions/active").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.heartRateAvgBpm").value(142));
    }
}
