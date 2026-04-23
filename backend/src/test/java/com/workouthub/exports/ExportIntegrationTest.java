package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ExportIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "ExportSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/export/claude-summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void summaryBundlesRecentFinishedSessionsAndTotalsForTheCaller() throws Exception {
        SeededUser user = helpers.seed(
                "exp-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        // Run a session: 2 sets of 10 reps at 50kg = 1000 volume.
        UUID sessionId = startSession(auth);
        addSet(auth, sessionId, exerciseId, 1, 10, 50.0);
        addSet(auth, sessionId, exerciseId, 2, 10, 50.0);
        finishSession(auth, sessionId);

        mvc.perform(get("/api/export/claude-summary")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(jsonPath("$.user.email").value(user.email()))
                .andExpect(jsonPath("$.period.days").value(30))
                .andExpect(jsonPath("$.summary.totalWorkouts").value(1))
                .andExpect(jsonPath("$.summary.totalVolumeKg").value(1000.00))
                .andExpect(jsonPath("$.workouts.length()").value(1))
                .andExpect(jsonPath("$.workouts[0].exercises[0].sets.length()").value(2))
                .andExpect(jsonPath("$.workouts[0].exercises[0].sets[0].repsDone").value(10))
                .andExpect(jsonPath("$.workouts[0].exercises[0].sets[0].weightKg").value(50.0));
    }

    @Test
    void summaryDoesNotIncludeOtherUsersSessions() throws Exception {
        SeededUser mine = helpers.seed(
                "mine-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser other = helpers.seed(
                "other-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID exerciseId = seedExercise();

        UUID otherSessionId = startSession("Bearer " + other.accessToken());
        addSet("Bearer " + other.accessToken(), otherSessionId, exerciseId, 1, 10, 50.0);
        finishSession("Bearer " + other.accessToken(), otherSessionId);

        mvc.perform(get("/api/export/claude-summary")
                        .header("Authorization", "Bearer " + mine.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalWorkouts").value(0))
                .andExpect(jsonPath("$.workouts.length()").value(0));
    }

    @Test
    void daysQueryParamClampedBelowOneReturns400() throws Exception {
        SeededUser user = helpers.seed(
                "d0-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/export/claude-summary?days=0")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isBadRequest());
    }

    private UUID seedExercise() {
        String tag = "Exp-" + System.nanoTime();
        Exercise e = new Exercise();
        e.setNameTr("TR " + tag);
        e.setNameEn("EN " + tag);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.DUMBBELL);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.INTERMEDIATE);
        return exerciseRepo.save(e).getId();
    }

    private UUID startSession(String auth) throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
    }

    private void addSet(String auth, UUID sessionId, UUID exerciseId,
                        int setNumber, int reps, double weight) throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":" + setNumber + ","
                                + "\"repsDone\":" + reps + ","
                                + "\"weightKg\":" + weight + "}"))
                .andExpect(status().isCreated());
    }

    private void finishSession(String auth, UUID sessionId) throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
