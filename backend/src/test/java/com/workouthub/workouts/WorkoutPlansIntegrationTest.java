package com.workouthub.workouts;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class WorkoutPlansIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "PlanSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void unauthenticatedListReturns401() throws Exception {
        mvc.perform(get("/api/workout-plans")).andExpect(status().isUnauthorized());
    }

    @Test
    void createListAndFetchPlanRoundTrip() throws Exception {
        SeededUser user = helpers.seed(
                "crud-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        MvcResult created = mvc.perform(post("/api/workout-plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Starter Plan\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Starter Plan"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.days.length()").value(0))
                .andReturn();
        UUID planId = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/workout-plans").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$[0].id").value(planId.toString()));

        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Starter Plan"));
    }

    @Test
    void updateChangesName() throws Exception {
        SeededUser user = helpers.seed(
                "upd-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID planId = createPlan(auth, "Old Name");

        mvc.perform(put("/api/workout-plans/" + planId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void deleteRemovesPlanAndSubsequentGetReturns404() throws Exception {
        SeededUser user = helpers.seed(
                "del-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID planId = createPlan(auth, "Disposable");

        mvc.perform(delete("/api/workout-plans/" + planId).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotSeeAnotherUsersPlan() throws Exception {
        SeededUser ownerUser = helpers.seed(
                "owner-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String ownerAuth = "Bearer " + ownerUser.accessToken();
        UUID planId = createPlan(ownerAuth, "Owner's Plan");

        SeededUser intruderUser = helpers.seed(
                "intruder-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String intruderAuth = "Bearer " + intruderUser.accessToken();

        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", intruderAuth))
                .andExpect(status().isNotFound());

        mvc.perform(put("/api/workout-plans/" + planId)
                        .header("Authorization", intruderAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hijacked\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/workout-plans/" + planId).header("Authorization", intruderAuth))
                .andExpect(status().isNotFound());

        // Owner still sees it.
        mvc.perform(get("/api/workout-plans/" + planId).header("Authorization", ownerAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Owner's Plan"));
    }

    @Test
    void unknownIdReturns404() throws Exception {
        SeededUser user = helpers.seed(
                "unk-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/workout-plans/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void emptyNameOnCreateReturns400() throws Exception {
        SeededUser user = helpers.seed(
                "empty-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/workout-plans")
                        .header("Authorization", "Bearer " + user.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field=='name')]").exists());
    }

    private UUID createPlan(String auth, String name) throws Exception {
        MvcResult r = mvc.perform(post("/api/workout-plans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(
                r.getResponse().getContentAsString()).get("id").asText());
    }
}
