package com.workouthub.analytics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class PrDetectionIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "PrSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    @Test
    void firstEverCompletedSetIsFlaggedAsPr() throws Exception {
        SeededUser u = helpers.seed(
                "pr1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        UUID exerciseId = seedExercise();
        UUID sessionId = startSession(auth);

        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":1,\"repsDone\":5,\"weightKg\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newPr").value(true));
    }

    @Test
    void setThatDoesNotBeatPriorFinishedBestIsNotFlagged() throws Exception {
        SeededUser u = helpers.seed(
                "pr2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        UUID exerciseId = seedExercise();

        UUID s1 = startSession(auth);
        mvc.perform(post("/api/sessions/" + s1 + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":1,\"repsDone\":5,\"weightKg\":100}"))
                .andExpect(status().isCreated());
        finish(auth, s1);

        UUID s2 = startSession(auth);
        mvc.perform(post("/api/sessions/" + s2 + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":1,\"repsDone\":5,\"weightKg\":95}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newPr").doesNotExist());
    }

    @Test
    void setThatBeatsPriorFinishedBestIsFlaggedPr() throws Exception {
        SeededUser u = helpers.seed(
                "pr3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        UUID exerciseId = seedExercise();

        UUID s1 = startSession(auth);
        mvc.perform(post("/api/sessions/" + s1 + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":1,\"repsDone\":5,\"weightKg\":100}"))
                .andExpect(status().isCreated());
        finish(auth, s1);

        UUID s2 = startSession(auth);
        mvc.perform(post("/api/sessions/" + s2 + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":1,\"repsDone\":5,\"weightKg\":105}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newPr").value(true));

        mvc.perform(get("/api/sessions/" + s2).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets[0].newPr").value(true));
    }

    @Test
    void prSurvivesUpdateThatStillBeatsPrior() throws Exception {
        SeededUser u = helpers.seed(
                "pr4-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        UUID exerciseId = seedExercise();

        UUID s1 = startSession(auth);
        addSet(auth, s1, exerciseId, 1, 5, 100);
        finish(auth, s1);

        UUID s2 = startSession(auth);
        UUID s2SetId = addSet(auth, s2, exerciseId, 1, 5, 110);

        // Patch the active s2 set to 6 reps @ 110kg.
        // Epley(110,6)=132 still beats Epley(100,5)=116.667 -> PR retained.
        mvc.perform(put("/api/sessions/" + s2 + "/sets/" + s2SetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repsDone\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newPr").value(true));

        mvc.perform(get("/api/sessions/" + s2).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets[0].newPr").value(true));
    }

    @Test
    void prClearedWhenUpdatedBelowPriorAndPromotesNextBest() throws Exception {
        SeededUser u = helpers.seed(
                "pr5-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        UUID exerciseId = seedExercise();

        UUID s1 = startSession(auth);
        addSet(auth, s1, exerciseId, 1, 5, 100);
        finish(auth, s1);

        UUID s2 = startSession(auth);
        UUID s2SetId = addSet(auth, s2, exerciseId, 1, 5, 110);

        // Patch the active s2 set to 1 rep @ 50kg.
        // Epley(50,1)=50 below Epley(100,5)=116.667 -> s2 demoted, s1 re-elected.
        mvc.perform(put("/api/sessions/" + s2 + "/sets/" + s2SetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repsDone\":1,\"weightKg\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newPr").doesNotExist());

        mvc.perform(get("/api/sessions/" + s1).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets[0].newPr").value(true));

        mvc.perform(get("/api/sessions/" + s2).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sets[0].newPr").doesNotExist());
    }

    private UUID addSet(String auth, UUID sessionId, UUID exerciseId,
                        int setNumber, int reps, double weight) throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\","
                                + "\"setNumber\":" + setNumber
                                + ",\"repsDone\":" + reps
                                + ",\"weightKg\":" + weight + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID seedExercise() {
        String tag = "pr-" + System.nanoTime();
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

    private void finish(String auth, UUID sessionId) throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
