package com.workouthub.workouts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class WorkoutDaysIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "DaySecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired ExerciseRepository exerciseRepo;

    private String auth;
    private UUID planId;

    @BeforeEach
    void setUp() throws Exception {
        SeededUser u = helpers.seed(
                "day-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        this.auth = "Bearer " + u.accessToken();
        MvcResult p = mvc.perform(post("/api/workout-plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"My Plan\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        this.planId = UUID.fromString(objectMapper.readTree(
                p.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void createDayAddsItToPlan() throws Exception {
        mvc.perform(post("/api/workout-plans/" + planId + "/days")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":1,"name":"Monday Push","focus":"push","estimatedDurationMin":45}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value(1))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.focus").value("push"));

        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(1));
    }

    @Test
    void duplicateDayOfWeekReturns409() throws Exception {
        createDay(1, "First", "push");
        mvc.perform(post("/api/workout-plans/" + planId + "/days")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":1,"name":"Second","focus":"pull"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void dayOfWeekOutOfRangeReturns400() throws Exception {
        mvc.perform(post("/api/workout-plans/" + planId + "/days")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dayOfWeek":8,"name":"Bad","focus":"push"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItemsAndReorderCloseNoGaps() throws Exception {
        UUID dayId = createDay(1, "Push", "push");
        UUID ex1 = seedExercise("Ex1-");
        UUID ex2 = seedExercise("Ex2-");
        UUID ex3 = seedExercise("Ex3-");

        UUID item1 = addItem(dayId, ex1);
        UUID item2 = addItem(dayId, ex2);
        UUID item3 = addItem(dayId, ex3);

        mvc.perform(post("/api/workout-plans/" + planId + "/days/" + dayId + "/exercises/reorder")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemIdsInOrder\":[\"" + item3 + "\",\"" + item1 + "\",\"" + item2 + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].id").value(item3.toString()))
                .andExpect(jsonPath("$.exercises[0].orderIndex").value(1))
                .andExpect(jsonPath("$.exercises[1].id").value(item1.toString()))
                .andExpect(jsonPath("$.exercises[2].id").value(item2.toString()));
    }

    @Test
    void reorderWithIncompleteListReturns409() throws Exception {
        UUID dayId = createDay(2, "Pull", "pull");
        UUID ex1 = seedExercise("In1-");
        UUID ex2 = seedExercise("In2-");
        UUID item1 = addItem(dayId, ex1);
        addItem(dayId, ex2);

        mvc.perform(post("/api/workout-plans/" + planId + "/days/" + dayId + "/exercises/reorder")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemIdsInOrder\":[\"" + item1 + "\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteItemRenumbersRemaining() throws Exception {
        UUID dayId = createDay(3, "Legs", "legs");
        UUID ex1 = seedExercise("Ren1-");
        UUID ex2 = seedExercise("Ren2-");
        UUID ex3 = seedExercise("Ren3-");
        addItem(dayId, ex1);
        UUID item2 = addItem(dayId, ex2);
        addItem(dayId, ex3);

        mvc.perform(delete("/api/workout-plans/" + planId + "/days/" + dayId
                        + "/exercises/" + item2).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days[0].exercises.length()").value(2))
                .andExpect(jsonPath("$.days[0].exercises[0].orderIndex").value(1))
                .andExpect(jsonPath("$.days[0].exercises[1].orderIndex").value(2));
    }

    @Test
    void deleteDayCascadesItems() throws Exception {
        UUID dayId = createDay(4, "Core", "core");
        UUID ex1 = seedExercise("Cas1-");
        addItem(dayId, ex1);

        mvc.perform(delete("/api/workout-plans/" + planId + "/days/" + dayId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(0));
    }

    @Test
    void updateItemPatchesFields() throws Exception {
        UUID dayId = createDay(5, "Cardio", "cardio");
        UUID ex = seedExercise("Upd-");
        UUID itemId = addItem(dayId, ex);

        mvc.perform(put("/api/workout-plans/" + planId + "/days/" + dayId
                        + "/exercises/" + itemId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetSets\":5,\"restSeconds\":90,\"notes\":\"hard set\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetSets").value(5))
                .andExpect(jsonPath("$.restSeconds").value(90))
                .andExpect(jsonPath("$.notes").value("hard set"));
    }

    private UUID createDay(int dayOfWeek, String name, String focus) throws Exception {
        MvcResult r = mvc.perform(post("/api/workout-plans/" + planId + "/days")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":" + dayOfWeek + ",\"name\":\"" + name
                                + "\",\"focus\":\"" + focus + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID addItem(UUID dayId, UUID exerciseId) throws Exception {
        MvcResult r = mvc.perform(post("/api/workout-plans/" + planId + "/days/"
                        + dayId + "/exercises")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseId\":\"" + exerciseId + "\",\"targetSets\":3}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID seedExercise(String tag) {
        String unique = tag + System.nanoTime();
        Exercise e = new Exercise();
        e.setNameTr("TR " + unique);
        e.setNameEn("EN " + unique);
        e.setCategory(Category.PUSH);
        e.setEquipment(Equipment.DUMBBELL);
        e.setMusclePrimary("chest");
        e.setDifficulty(Difficulty.BEGINNER);
        return exerciseRepo.save(e).getId();
    }
}
