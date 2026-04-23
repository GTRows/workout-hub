package com.workouthub.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class UsersMeIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_SECRET = "Str0ngPass!";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void getMeRequiresAuth() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeReturnsAuthenticatedUser() throws Exception {
        String email = "me-" + System.nanoTime() + "@test.local";
        String token = registerAndExtractAccessToken(email, STRONG_SECRET, "Me User");

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Me User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void updateMePersistsProfileChanges() throws Exception {
        String email = "upd-" + System.nanoTime() + "@test.local";
        String token = registerAndExtractAccessToken(email, STRONG_SECRET, "Upd User");

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
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.profile.heightCm").value(178))
                .andExpect(jsonPath("$.profile.weightKg").value(77.5))
                .andExpect(jsonPath("$.profile.healthNotes").value("NAFLD"))
                .andExpect(jsonPath("$.profile.goals").value("Fit body"));

        // Read back to confirm persistence.
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Updated Name"))
                .andExpect(jsonPath("$.profile.heightCm").value(178));
    }

    @Test
    void updateMeRejectsOutOfRangeWeight() throws Exception {
        String email = "range-" + System.nanoTime() + "@test.local";
        String token = registerAndExtractAccessToken(email, STRONG_SECRET, "Range User");

        String body = "{\"weightKg\": 5.0}";

        mvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("weightKg"));
    }

    private String registerAndExtractAccessToken(String email, String secret, String displayName)
            throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + secret
                + "\",\"displayName\":\"" + displayName + "\"}";
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
