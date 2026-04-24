package com.workouthub.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class PasswordChangeIntegrationTest extends AbstractIntegrationTest {

    private static final String OLD_SECRET = "ChangeMeSecret1!";
    private static final String NEW_SECRET = "ReplacementPhrase1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void changingPasswordRevokesAllExistingRefreshTokens() throws Exception {
        String email = "pc-a-" + System.nanoTime() + "@test.local";
        helpers.seed(email, OLD_SECRET, Role.USER);

        // First login -> gets access + refresh.
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + OLD_SECRET + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = login.getResponse().getContentAsString();
        String access = objectMapper.readTree(body).get("accessToken").asText();
        String refresh = objectMapper.readTree(body).get("refreshToken").asText();

        // Change password.
        mvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + OLD_SECRET
                                + "\",\"newPassword\":\"" + NEW_SECRET + "\"}"))
                .andExpect(status().isNoContent());

        // Prior refresh token must be rejected.
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());

        // Old password no longer works.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + OLD_SECRET + "\"}"))
                .andExpect(status().isUnauthorized());

        // New password does.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + NEW_SECRET + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void wrongCurrentPasswordReturns401() throws Exception {
        String email = "pc-b-" + System.nanoTime() + "@test.local";
        helpers.seed(email, OLD_SECRET, Role.USER);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + OLD_SECRET + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        mvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"WrongPhrase1!\","
                                + "\"newPassword\":\"" + NEW_SECRET + "\"}"))
                .andExpect(status().isUnauthorized());
    }
}
