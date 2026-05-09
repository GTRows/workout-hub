package com.workouthub.workouts;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
class DefaultPlanSeederTest extends AbstractIntegrationTest {

    private static final String ADMIN_SECRET = "AdminSeeder1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkoutPlanRepository plans;
    @Autowired DefaultPlanSeeder seeder;

    @Test
    @Transactional
    void creatingUserViaAdminApiSeedsAnActiveStarterPlan() throws Exception {
        SeededUser admin = helpers.seed(
                "planadm-" + System.nanoTime() + "@test.local", ADMIN_SECRET, Role.ADMIN);
        String adminAuth = "Bearer " + admin.accessToken();

        String newEmail = "new-" + System.nanoTime() + "@test.local";
        MvcResult created = mvc.perform(post("/api/admin/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "NewUserPass1!",
                                  "displayName": "New User",
                                  "role": "USER"
                                }
                                """.formatted(newEmail)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID newUserId = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        assertThat(plans.findByUserIdAndActiveTrue(newUserId))
                .as("freshly created user should own exactly one active plan")
                .isPresent()
                .get()
                .satisfies(p -> {
                    assertThat(p.getName()).isEqualTo("Baslangic Plani");
                    assertThat(p.getDays()).hasSize(3);
                });
    }

    @Test
    void seedForIsIdempotent() {
        SeededUser user = helpers.seed(
                "idem-" + System.nanoTime() + "@test.local", "Secret1!", Role.USER);
        seeder.seedFor(user.id());
        seeder.seedFor(user.id());

        long active = plans.findByUserIdOrderByCreatedAtAsc(user.id())
                .stream()
                .filter(p -> p.isActive())
                .count();
        assertThat(active).isEqualTo(1L);
    }

    @Test
    void seededPlanIsVisibleViaPublicPlanListForTheNewUser() throws Exception {
        // Admin creates a user, then that user's JWT sees their plan in GET /api/workout-plans
        SeededUser admin = helpers.seed(
                "ls-" + System.nanoTime() + "@test.local", ADMIN_SECRET, Role.ADMIN);
        String adminAuth = "Bearer " + admin.accessToken();

        String newEmail = "lsnew-" + System.nanoTime() + "@test.local";
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "Pass1234!",
                                  "displayName": "X",
                                  "role": "USER"
                                }
                                """.formatted(newEmail)))
                .andExpect(status().isCreated());

        // Now log in as the new user and list plans
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + newEmail + "\",\"password\":\"Pass1234!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        mvc.perform(get("/api/workout-plans")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Baslangic Plani"))
                .andExpect(jsonPath("$[0].active").value(true));
    }
}
