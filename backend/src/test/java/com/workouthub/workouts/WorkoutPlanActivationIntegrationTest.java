package com.workouthub.workouts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class WorkoutPlanActivationIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "ActSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkoutPlanRepository plans;

    @Test
    void activateFlipsIsActiveAndDeactivatesPreviousActivePlan() throws Exception {
        SeededUser user = helpers.seed(
                "act-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        UUID planA = createPlan(auth, "Plan A");
        UUID planB = createPlan(auth, "Plan B");

        mvc.perform(post("/api/workout-plans/" + planA + "/activate")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        Assertions.assertThat(plans.findByUserIdAndActiveTrue(user.id()))
                .map(p -> p.getId())
                .contains(planA);

        mvc.perform(post("/api/workout-plans/" + planB + "/activate")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Exactly one active plan per user is enforced.
        Assertions.assertThat(plans.findByUserIdAndActiveTrue(user.id()))
                .map(p -> p.getId())
                .contains(planB);
        Assertions.assertThat(plans.findById(planA))
                .map(p -> p.isActive())
                .contains(false);
    }

    @Test
    void activatingAlreadyActivePlanIsIdempotent() throws Exception {
        SeededUser user = helpers.seed(
                "idem-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();
        UUID plan = createPlan(auth, "Only");

        mvc.perform(post("/api/workout-plans/" + plan + "/activate")
                        .header("Authorization", auth))
                .andExpect(status().isOk());
        mvc.perform(post("/api/workout-plans/" + plan + "/activate")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void activatingAnotherUsersPlanReturns404() throws Exception {
        SeededUser owner = helpers.seed(
                "own-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser intruder = helpers.seed(
                "int-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        UUID ownerPlan = createPlan("Bearer " + owner.accessToken(), "Owner");

        mvc.perform(post("/api/workout-plans/" + ownerPlan + "/activate")
                        .header("Authorization", "Bearer " + intruder.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void activatingUnknownPlanReturns404() throws Exception {
        SeededUser user = helpers.seed(
                "unk-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/workout-plans/" + UUID.randomUUID() + "/activate")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReflectsActiveFlag() throws Exception {
        SeededUser user = helpers.seed(
                "lst-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + user.accessToken();

        UUID planA = createPlan(auth, "A");
        createPlan(auth, "B");

        mvc.perform(post("/api/workout-plans/" + planA + "/activate")
                        .header("Authorization", auth))
                .andExpect(status().isOk());

        mvc.perform(get("/api/workout-plans").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.active==true)].id").value(
                        org.hamcrest.Matchers.contains(planA.toString())));
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
