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
class UsersMeIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_SECRET = "Str0ngPass!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void getMeRequiresAuth() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeReturnsAuthenticatedUser() throws Exception {
        SeededUser u = helpers.seed(
                "me-" + System.nanoTime() + "@test.local", STRONG_SECRET, Role.USER);

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(u.email()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void updateMePersistsProfileChanges() throws Exception {
        SeededUser u = helpers.seed(
                "upd-" + System.nanoTime() + "@test.local", STRONG_SECRET, Role.USER);

        String body = """
                {
                  "displayName": "Updated Name",
                  "heightCm": 178,
                  "weightKg": 77.5,
                  "healthNotes": "NAFLD",
                  "goals": "Fit body"
                }
                """;

        mvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.profile.heightCm").value(178))
                .andExpect(jsonPath("$.profile.weightKg").value(77.5))
                .andExpect(jsonPath("$.profile.healthNotes").value("NAFLD"))
                .andExpect(jsonPath("$.profile.goals").value("Fit body"));

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.profile.heightCm").value(178));
    }

    @Test
    void updateMeRejectsOutOfRangeWeight() throws Exception {
        SeededUser u = helpers.seed(
                "range-" + System.nanoTime() + "@test.local", STRONG_SECRET, Role.USER);

        mvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\": 5.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("weightKg"));
    }
}
