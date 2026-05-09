package com.workouthub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
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
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_SECRET = "Str0ngPass!";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TestAuthHelpers helpers;

    @Test
    void publicRegisterEndpointIsNotAvailable() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x@y.z\",\"password\":\"Str0ngPass!\",\"displayName\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRefreshRotationAgainstSeededUser() throws Exception {
        String email = "flow-" + System.nanoTime() + "@test.local";
        helpers.seed(email, STRONG_SECRET, Role.USER);

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, STRONG_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();
        String loginRefresh = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("refreshToken").asText();

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
                .andReturn();
        String rotated = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .get("refreshToken").asText();

        assertThat(rotated).isNotEqualTo(loginRefresh);

        // Re-using the already-rotated refresh token must fail.
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = "wrong-" + System.nanoTime() + "@test.local";
        helpers.seed(email, STRONG_SECRET, Role.USER);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "NotTheSecret!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithUnknownEmailReturns401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ghost-" + System.nanoTime() + "@test.local", STRONG_SECRET)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginValidationFailureReturns400() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private String loginBody(String email, String secret) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + secret + "\"}";
    }
}
