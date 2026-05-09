package com.workouthub.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
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
class AnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "AnalyticsSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    @Test
    void unauthenticatedCallReturns401() throws Exception {
        mvc.perform(get("/api/analytics/volume"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void volumeReturnsDefaultTwelveWeeksEvenWhenEmpty() throws Exception {
        SeededUser user = helpers.seed(
                "vol-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/analytics/volume")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].totalVolumeKg").value(0.0))
                .andExpect(jsonPath("$[0].sessionCount").value(0));
    }

    @Test
    void volumeIncludesVolumeFromARecentFinishedSession() throws Exception {
        SeededUser user = helpers.seed(
                "vh-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID sid = startSession(auth);
        addSet(auth, sid, exerciseId, 1, 10, 60.0);
        addSet(auth, sid, exerciseId, 2, 10, 60.0);
        finish(auth, sid);

        mvc.perform(get("/api/analytics/volume?weeks=4")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[3].totalVolumeKg").value(1200.00))
                .andExpect(jsonPath("$[3].sessionCount").value(1));
    }

    @Test
    void oneRmReturnsEpleyEstimatesPerSession() throws Exception {
        SeededUser user = helpers.seed(
                "orm-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID sid = startSession(auth);
        addSet(auth, sid, exerciseId, 1, 5, 100.0);
        finish(auth, sid);

        mvc.perform(get("/api/analytics/one-rm/" + exerciseId)
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estimatedOneRmKg").value(116.67))
                .andExpect(jsonPath("$[0].repsDone").value(5))
                .andExpect(jsonPath("$[0].weightKg").value(100.0));
    }

    @Test
    void oneRmForUnknownExerciseIs404() throws Exception {
        SeededUser user = helpers.seed(
                "unk-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/analytics/one-rm/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void streakIsZeroWhenNoSessions() throws Exception {
        SeededUser user = helpers.seed(
                "str-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/analytics/streak")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreakDays").value(0))
                .andExpect(jsonPath("$.longestStreakDays").value(0));
    }

    @Test
    void streakCountsASingleTodaySession() throws Exception {
        SeededUser user = helpers.seed(
                "s1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID sid = startSession(auth);
        addSet(auth, sid, exerciseId, 1, 5, 50.0);
        finish(auth, sid);

        mvc.perform(get("/api/analytics/streak")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreakDays").value(1))
                .andExpect(jsonPath("$.longestStreakDays").value(1));
    }

    @Test
    void prsReturnsEmptyListWhenNoHistory() throws Exception {
        SeededUser user = helpers.seed(
                "pr0-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/analytics/prs")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void prsReturnsBestEpleyPerExercise() throws Exception {
        SeededUser user = helpers.seed(
                "pr1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID sid = startSession(auth);
        addSet(auth, sid, exerciseId, 1, 10, 60.0);
        addSet(auth, sid, exerciseId, 2, 5, 80.0);
        addSet(auth, sid, exerciseId, 3, 8, 70.0);
        finish(auth, sid);

        mvc.perform(get("/api/analytics/prs")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].exerciseId").value(exerciseId.toString()))
                .andExpect(jsonPath("$[0].weightKg").value(80.0))
                .andExpect(jsonPath("$[0].repsDone").value(5));
    }

    @Test
    void heatmapReturnsOneEntryPerDayAcrossTheRequestedWeeks() throws Exception {
        SeededUser user = helpers.seed(
                "hm-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/analytics/heatmap?weeks=4")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(28));
    }

    @Test
    void heatmapMarksTodayWhenASessionIsFinishedToday() throws Exception {
        SeededUser user = helpers.seed(
                "hm2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID sid = startSession(auth);
        addSet(auth, sid, exerciseId, 1, 5, 50.0);
        finish(auth, sid);

        mvc.perform(get("/api/analytics/heatmap?weeks=1")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[?(@.sessionCount > 0)].sessionCount").isNotEmpty());
    }

    @Test
    void analyticsAreIsolatedPerUser() throws Exception {
        SeededUser a = helpers.seed(
                "ai-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser b = helpers.seed(
                "bi-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String aAuth = "Bearer " + a.accessToken();
        UUID exerciseId = seedExercise();

        UUID sid = startSession(aAuth);
        addSet(aAuth, sid, exerciseId, 1, 5, 100.0);
        finish(aAuth, sid);

        mvc.perform(get("/api/analytics/prs")
                        .header("Authorization", "Bearer " + b.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private UUID seedExercise() {
        String tag = "An-" + System.nanoTime();
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

    private void finish(String auth, UUID sessionId) throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
