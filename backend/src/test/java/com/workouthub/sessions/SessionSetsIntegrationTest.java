package com.workouthub.sessions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SessionSetsIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "SetSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    private String auth;
    private UUID sessionId;
    private UUID exerciseId;

    @BeforeEach
    void setUp() throws Exception {
        SeededUser u = helpers.seed(
                "sets-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        this.auth = "Bearer " + u.accessToken();
        this.sessionId = startSession();
        this.exerciseId = seedExercise();
    }

    @Test
    void addSetAutoAssignsSetNumberAndReturns201() throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 10, 60.0, 7)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setNumber").value(1))
                .andExpect(jsonPath("$.repsDone").value(10))
                .andExpect(jsonPath("$.completed").value(true));

        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 9, 60.0, 8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.setNumber").value(2));
    }

    @Test
    void duplicateExplicitSetNumberReturns409() throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(1, 10, 60.0, null)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(1, 8, 60.0, null)))
                .andExpect(status().isConflict());
    }

    @Test
    void rpeOutOfRangeReturns400() throws Exception {
        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 10, 60.0, 11)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAfterFinishReturns409() throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 10, 60.0, null)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID setId = UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/api/sessions/" + sessionId + "/finish")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mvc.perform(put("/api/sessions/" + sessionId + "/sets/" + setId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repsDone\":12}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updatePatchesFields() throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 10, 60.0, 6)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID setId = UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(put("/api/sessions/" + sessionId + "/sets/" + setId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"repsDone\":12,\"weightKg\":62.5,\"rpe\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.repsDone").value(12))
                .andExpect(jsonPath("$.weightKg").value(62.5))
                .andExpect(jsonPath("$.rpe").value(8));
    }

    @Test
    void deleteRemovesSet() throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 10, 60.0, null)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID setId = UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(delete("/api/sessions/" + sessionId + "/sets/" + setId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());
    }

    @Test
    void crossUserCannotAddToOthersSession() throws Exception {
        SeededUser intruder = helpers.seed(
                "ix-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/sessions/" + sessionId + "/sets")
                        .header("Authorization", "Bearer " + intruder.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setBody(null, 10, 60.0, null)))
                .andExpect(status().isNotFound());
    }

    private UUID startSession() throws Exception {
        MvcResult r = mvc.perform(post("/api/sessions/start")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID seedExercise() {
        String tag = "SetEx-" + System.nanoTime();
        Exercise e = new Exercise();
        e.setNameTr("TR " + tag);
        e.setNameEn("EN " + tag);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.DUMBBELL);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.BEGINNER);
        return exerciseRepo.save(e).getId();
    }

    private String setBody(Integer setNumber, int reps, double weight, Integer rpe) {
        return "{\"exerciseId\":\"" + exerciseId + "\""
                + (setNumber == null ? "" : ",\"setNumber\":" + setNumber)
                + ",\"repsDone\":" + reps
                + ",\"weightKg\":" + weight
                + (rpe == null ? "" : ",\"rpe\":" + rpe)
                + "}";
    }
}
