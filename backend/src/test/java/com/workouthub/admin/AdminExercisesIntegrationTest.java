package com.workouthub.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminExercisesIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_SECRET = "AdminSecret!";
    private static final String USER_SECRET = "UserSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    private String adminAuth;
    private String userAuth;

    @BeforeEach
    void setUp() {
        SeededUser admin = helpers.seed(
                "exadmin-" + System.nanoTime() + "@test.local", ADMIN_SECRET, Role.ADMIN);
        SeededUser user = helpers.seed(
                "exuser-" + System.nanoTime() + "@test.local", USER_SECRET, Role.USER);
        this.adminAuth = "Bearer " + admin.accessToken();
        this.userAuth = "Bearer " + user.accessToken();
    }

    @Test
    void unauthenticatedCreateReturns401() throws Exception {
        mvc.perform(post("/api/admin/exercises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody("Shove-" + System.nanoTime())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void plainUserCreateReturns403() throws Exception {
        mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody("Shove-" + System.nanoTime())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreateValidReturns201WithLowercaseEnums() throws Exception {
        mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody("UniqueEn-" + System.nanoTime())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("push"))
                .andExpect(jsonPath("$.equipment").value("bodyweight"))
                .andExpect(jsonPath("$.difficulty").value("beginner"));
    }

    @Test
    void adminCreateMissingTurkishNameReturns400() throws Exception {
        String body = """
                {
                  "nameEn": "Missing TR %d",
                  "category": "push",
                  "equipment": "bodyweight",
                  "musclePrimary": "chest",
                  "difficulty": "beginner"
                }
                """.formatted(System.nanoTime());

        mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='nameTr')]").exists());
    }

    @Test
    void adminCreateMissingEnglishNameReturns400() throws Exception {
        String body = """
                {
                  "nameTr": "Eksik EN %d",
                  "category": "push",
                  "equipment": "bodyweight",
                  "musclePrimary": "chest",
                  "difficulty": "beginner"
                }
                """.formatted(System.nanoTime());

        mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='nameEn')]").exists());
    }

    @Test
    void adminDuplicateEnglishNameReturns409() throws Exception {
        String shared = "Dup-" + System.nanoTime();

        mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody(shared)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody(shared)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminUpdatePartialPersistsAndReturnsMergedDoc() throws Exception {
        MvcResult created = mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody("Upd-" + System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(put("/api/admin/exercises/" + id)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"advanced\",\"muscleSecondary\":\"triceps\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("advanced"))
                .andExpect(jsonPath("$.muscleSecondary").value("triceps"))
                .andExpect(jsonPath("$.category").value("push"));
    }

    @Test
    void adminUpdateUnknownIdReturns404() throws Exception {
        mvc.perform(put("/api/admin/exercises/" + UUID.randomUUID())
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"advanced\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminDeleteUnreferencedReturns204() throws Exception {
        MvcResult created = mvc.perform(post("/api/admin/exercises")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalCreateBody("DelMe-" + System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(delete("/api/admin/exercises/" + id).header("Authorization", adminAuth))
                .andExpect(status().isNoContent());
    }

    private String minimalCreateBody(String nameEn) {
        return """
                {
                  "nameTr": "%s-TR",
                  "nameEn": "%s",
                  "category": "push",
                  "equipment": "bodyweight",
                  "musclePrimary": "chest",
                  "difficulty": "beginner"
                }
                """.formatted(nameEn, nameEn);
    }
}
