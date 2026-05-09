package com.workouthub.exports;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
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
import java.time.LocalDate;
import java.time.ZoneOffset;
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
        UUID sessionId = startSession(auth, null);
        addSet(auth, sessionId, exerciseId, 1, 10, 50.0);
        addSet(auth, sessionId, exerciseId, 2, 10, 50.0);
        finishSession(auth, sessionId);

        mvc.perform(get("/api/export/claude-summary")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andExpect(jsonPath("$.period.days").value(30))
                .andExpect(jsonPath("$.summary.total_workouts").value(1))
                .andExpect(jsonPath("$.summary.total_volume_kg").value(1000.00))
                .andExpect(jsonPath("$.summary.planned_workouts").doesNotExist())
                .andExpect(jsonPath("$.summary.adherence_percent").doesNotExist())
                .andExpect(jsonPath("$.summary.weight_change_kg").doesNotExist())
                .andExpect(jsonPath("$.workouts.length()").value(1))
                .andExpect(jsonPath("$.workouts[0].exercises[0].sets.length()").value(2))
                .andExpect(jsonPath("$.workouts[0].exercises[0].sets[0].reps").value(10))
                .andExpect(jsonPath("$.workouts[0].exercises[0].sets[0].weight").value(50.0))
                .andExpect(jsonPath("$.prs").isArray())
                .andExpect(jsonPath("$.prs.length()").value(1))
                .andExpect(jsonPath("$.prs[0].weight").value(50.0))
                .andExpect(jsonPath("$.prs[0].reps").value(10))
                .andExpect(jsonPath("$.body_metrics").isArray())
                .andExpect(jsonPath("$.body_metrics.length()").value(0))
                .andExpect(jsonPath("$.consistency.current_streak_days").value(1))
                .andExpect(jsonPath("$.consistency.missed_reasons").isArray())
                .andExpect(jsonPath("$.consistency.missed_reasons.length()").value(0));
    }

    @Test
    void summaryDoesNotIncludeOtherUsersSessions() throws Exception {
        SeededUser mine = helpers.seed(
                "mine-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser other = helpers.seed(
                "other-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID exerciseId = seedExercise();

        UUID otherSessionId = startSession("Bearer " + other.accessToken(), null);
        addSet("Bearer " + other.accessToken(), otherSessionId, exerciseId, 1, 10, 50.0);
        finishSession("Bearer " + other.accessToken(), otherSessionId);

        mvc.perform(get("/api/export/claude-summary")
                        .header("Authorization", "Bearer " + mine.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.total_workouts").value(0))
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

    @Test
    void summaryIncludesAgeGoalsArrayAndWorkoutTypeWhenProfileAndPlanAreSeeded()
            throws Exception {
        SeededUser user = helpers.seed(
                "age-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        // Profile: birth date 2000-04-26, multi-line + semicolon-mixed goals.
        updateProfile(auth, """
                {
                  "displayName": "Age Goals Tester",
                  "heightCm": 178,
                  "weightKg": 78.0,
                  "birthDate": "2000-04-26",
                  "goals": "Fit vucut\\nKaraciger iyilestirme"
                }
                """);

        // Plan with one non-REST day matching today's day-of-week.
        short todayDow = (short) LocalDate.now(ZoneOffset.UTC).getDayOfWeek().getValue();
        UUID planId = createPlan(auth, "Test Plan");
        UUID dayId = createDay(auth, planId, todayDow, "Ust Vucut Itme", "PUSH");
        activatePlan(auth, planId);

        UUID sessionId = startSession(auth, dayId);
        addSet(auth, sessionId, exerciseId, 1, 8, 60.0);
        finishSession(auth, sessionId);

        mvc.perform(get("/api/export/claude-summary")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").doesNotExist())
                .andExpect(jsonPath("$.user.age").value(26))
                .andExpect(jsonPath("$.user.goals.length()").value(2))
                .andExpect(jsonPath("$.user.goals[0]").value("Fit vucut"))
                .andExpect(jsonPath("$.user.goals[1]").value("Karaciger iyilestirme"))
                .andExpect(jsonPath("$.workouts[0].type").value("Ust Vucut Itme"))
                .andExpect(jsonPath("$.workouts[0].energy_level").doesNotExist())
                .andExpect(jsonPath("$.summary.planned_workouts").isNumber())
                .andExpect(jsonPath("$.summary.adherence_percent").isNumber());
    }

    @Test
    void summaryIncludesBodyMetricsAndWeightChangeWhenMetricsSeededInWindow()
            throws Exception {
        SeededUser user = helpers.seed(
                "metrics-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate earlier = today.minusDays(28);

        upsertMetric(auth, earlier, 80.0);
        upsertMetric(auth, today, 78.5);

        mvc.perform(get("/api/export/claude-summary?days=30")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body_metrics.length()").value(2))
                .andExpect(jsonPath("$.body_metrics[0].weight_kg").value(78.5))
                .andExpect(jsonPath("$.body_metrics[1].weight_kg").value(80.0))
                .andExpect(jsonPath("$.summary.weight_change_kg").value(-1.50));
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

    private UUID startSession(String auth, UUID workoutDayId) throws Exception {
        String body = workoutDayId == null
                ? "{}"
                : "{\"workoutDayId\":\"" + workoutDayId + "\"}";
        MvcResult r = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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

    private void updateProfile(String auth, String json) throws Exception {
        mvc.perform(put("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    private UUID createPlan(String auth, String name) throws Exception {
        MvcResult r = mvc.perform(post("/api/workout-plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private UUID createDay(String auth, UUID planId, short dayOfWeek,
                           String name, String focus) throws Exception {
        MvcResult r = mvc.perform(post("/api/workout-plans/" + planId + "/days")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":" + dayOfWeek + ","
                                + "\"name\":\"" + name + "\","
                                + "\"focus\":\"" + focus + "\","
                                + "\"estimatedDurationMin\":60}"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(r.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private void activatePlan(String auth, UUID planId) throws Exception {
        mvc.perform(post("/api/workout-plans/" + planId + "/activate")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private void upsertMetric(String auth, LocalDate date, double weightKg) throws Exception {
        mvc.perform(post("/api/metrics")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"" + date + "\","
                                + "\"weightKg\":" + weightKg + "}"))
                .andExpect(status().is2xxSuccessful());
    }
}
