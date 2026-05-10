package com.workouthub.exercises;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class ExerciseEndpointsIntegrationTest extends AbstractIntegrationTest {

    private static final String USER_SECRET = "UserSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ExerciseRepository repo;

    private String authHeader;
    private UUID seededPushId;

    @BeforeEach
    void setUp() {
        SeededUser u = helpers.seed(
                "ex-" + System.nanoTime() + "@test.local", USER_SECRET, Role.USER);
        this.authHeader = "Bearer " + u.accessToken();

        Exercise pushUp = build("Sinav", "Push-up " + System.nanoTime(),
                Category.PUSH, Equipment.BODYWEIGHT, Difficulty.BEGINNER,
                List.of("Keep core tight"), List.of("Flared elbows"));
        this.seededPushId = repo.save(pushUp).getId();

        repo.save(build("Kurek", "Dumbbell Row " + System.nanoTime(),
                Category.PULL, Equipment.DUMBBELL, Difficulty.INTERMEDIATE,
                List.of("Retract scapula"), List.of("Rounding back")));
    }

    @Test
    void unauthenticatedListReturns401() throws Exception {
        mvc.perform(get("/api/exercises")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedListReturnsPagedResults() throws Exception {
        mvc.perform(get("/api/exercises").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.content[0].category").exists());
    }

    @Test
    void filterByCategoryNarrowsResults() throws Exception {
        mvc.perform(get("/api/exercises?category=push").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].category")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.equalTo("push"))));
    }

    @Test
    void getByIdReturnsExercise() throws Exception {
        mvc.perform(get("/api/exercises/" + seededPushId).header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("push"))
                .andExpect(jsonPath("$.difficulty").value("beginner"));
    }

    @Test
    void getByUnknownIdReturns404() throws Exception {
        mvc.perform(get("/api/exercises/" + UUID.randomUUID())
                        .header("Authorization", authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchByNameReturnsMatches() throws Exception {
        Exercise marker = build("Marker " + System.nanoTime(), "QuiteUniqueName",
                Category.CORE, Equipment.BODYWEIGHT, Difficulty.BEGINNER,
                List.of(), List.of());
        repo.save(marker);

        mvc.perform(get("/api/exercises/search?q=QuiteUniqueName")
                        .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void searchWithoutQReturns400() throws Exception {
        mvc.perform(get("/api/exercises/search").header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCategoryReturns400() throws Exception {
        mvc.perform(get("/api/exercises?category=not-a-real-category")
                        .header("Authorization", authHeader))
                .andExpect(status().isBadRequest());
    }

    private static Exercise build(
            String nameTr, String nameEn, Category category, Equipment equipment,
            Difficulty difficulty, List<String> tips, List<String> mistakes) {
        Exercise e = new Exercise();
        e.setNameTr(nameTr);
        e.setNameEn(nameEn);
        e.setCategory(category);
        e.setEquipment(equipment);
        e.setMusclePrimary("chest");
        e.setDifficulty(difficulty);
        e.setFormTipsEn(tips);
        e.setCommonMistakesEn(mistakes);
        return e;
    }
}
