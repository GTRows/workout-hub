package com.workouthub.analytics;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
class ExerciseAnalyticsIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "AnalSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    @Test
    void lastPerformanceReturns204WhenNoHistory() throws Exception {
        SeededUser user = helpers.seed(
                "lp0-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID exerciseId = seedExercise();

        mvc.perform(get("/api/exercises/" + exerciseId + "/last-performance")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void lastPerformanceReturnsMostRecentFinishedSessionSets() throws Exception {
        SeededUser user = helpers.seed(
                "lp-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        // First finished session: 1 set, 8 reps @ 60kg
        UUID s1 = startSession(auth);
        addSet(auth, s1, exerciseId, 1, 8, 60.0);
        finish(auth, s1);

        // Second finished session: 3 sets @ 65kg
        UUID s2 = startSession(auth);
        addSet(auth, s2, exerciseId, 1, 8, 65.0);
        addSet(auth, s2, exerciseId, 2, 8, 65.0);
        addSet(auth, s2, exerciseId, 3, 7, 65.0);
        finish(auth, s2);

        mvc.perform(get("/api/exercises/" + exerciseId + "/last-performance")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(s2.toString()))
                .andExpect(jsonPath("$.sets.length()").value(3))
                .andExpect(jsonPath("$.sets[0].setNumber").value(1));
    }

    @Test
    void progressReturnsPerSessionSummariesNewestFirst() throws Exception {
        SeededUser user = helpers.seed(
                "pr-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID s1 = startSession(auth);
        addSet(auth, s1, exerciseId, 1, 10, 50.0);
        addSet(auth, s1, exerciseId, 2, 10, 50.0);
        finish(auth, s1);

        UUID s2 = startSession(auth);
        addSet(auth, s2, exerciseId, 1, 12, 55.0);
        finish(auth, s2);

        mvc.perform(get("/api/exercises/" + exerciseId + "/progress?limit=10")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sessionId").value(s2.toString()))
                .andExpect(jsonPath("$[0].setCount").value(1))
                .andExpect(jsonPath("$[0].maxWeightKg").value(55.0))
                .andExpect(jsonPath("$[0].topRepsDone").value(12))
                // Epley(55kg, 12) = 55 * (1 + 12/30) = 77.000 (PrDetector setScale(3, HALF_UP))
                .andExpect(jsonPath("$[0].estimatedOneRmKg").value(77.000))
                .andExpect(jsonPath("$[1].sessionId").value(s1.toString()))
                .andExpect(jsonPath("$[1].setCount").value(2))
                .andExpect(jsonPath("$[1].totalVolumeKg").value(1000.00))
                // Both s1 sets identical (10 reps @ 50kg) so max-Epley = Epley(50, 10) = 66.667
                .andExpect(jsonPath("$[1].estimatedOneRmKg").value(66.667));
    }

    @Test
    void progressLimitClampedByLimitParam() throws Exception {
        SeededUser user = helpers.seed(
                "lim-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        for (int i = 0; i < 3; i++) {
            UUID s = startSession(auth);
            addSet(auth, s, exerciseId, 1, 10, 60.0);
            finish(auth, s);
        }

        mvc.perform(get("/api/exercises/" + exerciseId + "/progress?limit=2")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void progressOnAnotherUsersExerciseIsEmptyForCaller() throws Exception {
        SeededUser owner = helpers.seed(
                "own-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser other = helpers.seed(
                "ot-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String ownerAuth = "Bearer " + owner.accessToken();
        UUID exerciseId = seedExercise();

        UUID s = startSession(ownerAuth);
        addSet(ownerAuth, s, exerciseId, 1, 10, 60.0);
        finish(ownerAuth, s);

        mvc.perform(get("/api/exercises/" + exerciseId + "/progress")
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/exercises/" + exerciseId + "/last-performance")
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void unknownExerciseIdReturns404() throws Exception {
        SeededUser user = helpers.seed(
                "unk-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/exercises/" + UUID.randomUUID() + "/last-performance")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNotFound());
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

    private void addUnweightedSet(String auth, UUID sessionId, UUID exerciseId,
                                  int setNumber, int reps) throws Exception {
        // weightKg omitted from body so AddSetRequest.weightKg() resolves to null
        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":" + setNumber + ","
                                + "\"repsDone\":" + reps + "}"))
                .andExpect(status().isCreated());
    }

    private void finish(String auth, UUID sessionId) throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @org.junit.jupiter.api.Test
    void limitCountIsAtLeastOne() throws Exception {
        SeededUser user = helpers.seed(
                "zlim-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID exerciseId = seedExercise();

        mvc.perform(get("/api/exercises/" + exerciseId + "/progress?limit=0")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isBadRequest());

        // Ensure positive limit still works (no data -> empty page)
        mvc.perform(get("/api/exercises/" + exerciseId + "/progress?limit=1")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void progressEstimatedOneRmIsNullForUnweightedSets() throws Exception {
        SeededUser user = helpers.seed(
                "ep0-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID exerciseId = seedExercise();

        UUID s = startSession(auth);
        addUnweightedSet(auth, s, exerciseId, 1, 8);
        finish(auth, s);

        // Jackson NON_NULL strips null BigDecimal fields (maxWeightKg, estimatedOneRmKg)
        // so assert via doesNotExist rather than nullValue matcher.
        mvc.perform(get("/api/exercises/" + exerciseId + "/progress?limit=10")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].setCount").value(1))
                .andExpect(jsonPath("$[0].topRepsDone").value(8))
                .andExpect(jsonPath("$[0].maxWeightKg").doesNotExist())
                .andExpect(jsonPath("$[0].estimatedOneRmKg").doesNotExist());
    }
}
