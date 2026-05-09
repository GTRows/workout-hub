package com.workouthub.sessions.resttimer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
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

@AutoConfigureMockMvc
class RestTimerControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "RestCtrlSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired RestTimerScheduleRepository repository;

    @Test
    void unauthenticatedScheduleReturns401() throws Exception {
        mvc.perform(post("/api/sessions/" + UUID.randomUUID() + "/rest-timer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":60,\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scheduleReturns201AndPersistsRow() throws Exception {
        SeededUser u = helpers.seed(
                "rtc-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        mvc.perform(post("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":60,\"title\":\"Rest over\","
                                + "\"body\":\"60s rest complete\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.fireAt").exists());

        assertThat(repository.findByUserIdAndSessionId(u.id(), sessionId)).isPresent();
    }

    @Test
    void scheduleReturns404WhenSessionNotOwned() throws Exception {
        SeededUser owner = helpers.seed(
                "rto-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser intruder = helpers.seed(
                "rti-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(owner.id());

        mvc.perform(post("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + intruder.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":60,\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void scheduleReturns400OnZeroSeconds() throws Exception {
        SeededUser u = helpers.seed(
                "rt0-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        mvc.perform(post("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":0,\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleReturns400OnTooManySeconds() throws Exception {
        SeededUser u = helpers.seed(
                "rtX-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        mvc.perform(post("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":3601,\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void scheduleReturns400OnBlankTitle() throws Exception {
        SeededUser u = helpers.seed(
                "rtb-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        mvc.perform(post("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":60,\"title\":\"\",\"body\":\"b\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelReturns204AndRemovesRow() throws Exception {
        SeededUser u = helpers.seed(
                "rtd-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        mvc.perform(post("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seconds\":60,\"title\":\"t\",\"body\":\"b\"}"))
                .andExpect(status().isCreated());
        assertThat(repository.findByUserIdAndSessionId(u.id(), sessionId)).isPresent();

        mvc.perform(delete("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(repository.findByUserIdAndSessionId(u.id(), sessionId)).isEmpty();
    }

    @Test
    void cancelIsIdempotentWhenNoRowExists() throws Exception {
        SeededUser u = helpers.seed(
                "rtni-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        mvc.perform(delete("/api/sessions/" + sessionId + "/rest-timer")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isNoContent());
    }

    private UUID startSession(UUID userId) {
        WorkoutSession s = new WorkoutSession();
        s.setUserId(userId);
        return sessions.save(s).getId();
    }
}
