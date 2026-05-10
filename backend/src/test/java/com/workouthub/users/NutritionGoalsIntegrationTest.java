package com.workouthub.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class NutritionGoalsIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "GoalsSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void putAcceptsNutritionGoalsAndGetReadsThemBack() throws Exception {
        SeededUser u = helpers.seed(
                "g-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(put("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dailyKcalGoal": 2400,
                                  "dailyProteinGGoal": 180,
                                  "dailyCarbsGGoal": 240,
                                  "dailyFatGGoal": 80
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.dailyKcalGoal").value(2400))
                .andExpect(jsonPath("$.profile.dailyProteinGGoal").value(180));

        mvc.perform(get("/api/users/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.dailyCarbsGGoal").value(240))
                .andExpect(jsonPath("$.profile.dailyFatGGoal").value(80));
    }

    @Test
    void rejectsImpossibleKcalGoal() throws Exception {
        SeededUser u = helpers.seed(
                "g2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyKcalGoal\":999999}"))
                .andExpect(status().isBadRequest());
    }
}
