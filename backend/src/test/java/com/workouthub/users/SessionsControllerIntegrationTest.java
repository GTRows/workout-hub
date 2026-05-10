package com.workouthub.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SessionsControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "SessionSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void listReturnsTheActiveRefreshTokenAfterLogin() throws Exception {
        String email = "s-a-" + System.nanoTime() + "@test.local";
        helpers.seed(email, SECRET, Role.USER);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .header("User-Agent", "IntegrationTestAgent/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SECRET + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String access = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        mvc.perform(get("/api/users/me/sessions")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userAgent").value("IntegrationTestAgent/1.0"))
                .andExpect(jsonPath("$[0].id").isNotEmpty());
    }

    @Test
    void revokingASessionMakesSubsequentRefreshFail() throws Exception {
        String email = "s-b-" + System.nanoTime() + "@test.local";
        helpers.seed(email, SECRET, Role.USER);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .header("User-Agent", "TestAgent/2.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + SECRET + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = login.getResponse().getContentAsString();
        String access = objectMapper.readTree(body).get("accessToken").asText();
        String refresh = objectMapper.readTree(body).get("refreshToken").asText();

        MvcResult listed = mvc.perform(get("/api/users/me/sessions")
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper
                .readTree(listed.getResponse().getContentAsString())
                .get(0).get("id").asText());

        mvc.perform(delete("/api/users/me/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + access))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokingAnotherUsersSessionReturns404() throws Exception {
        SeededUser a = helpers.seed(
                "s-x-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser b = helpers.seed(
                "s-y-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        // A logs in so there's a refresh token row.
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + a.email() + "\",\"password\":\"" + SECRET + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String aAccess = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").asText();

        MvcResult listed = mvc.perform(get("/api/users/me/sessions")
                        .header("Authorization", "Bearer " + aAccess))
                .andExpect(status().isOk())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper
                .readTree(listed.getResponse().getContentAsString())
                .get(0).get("id").asText());

        // B tries to revoke A's session: must be 404.
        mvc.perform(delete("/api/users/me/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + b.accessToken()))
                .andExpect(status().isNotFound());
    }
}
