package com.workouthub.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_SECRET = "Str0ngPass!";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshFlow() throws Exception {
        String email = "flow-" + System.nanoTime() + "@test.local";

        MvcResult reg = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, STRONG_SECRET, "Flow User")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();
        JsonNode regBody = objectMapper.readTree(reg.getResponse().getContentAsString());
        String initialRefresh = regBody.get("refreshToken").asText();

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, STRONG_SECRET)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andReturn();
        String loginRefresh = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("refreshToken").asText();

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(notNullValue()))
                .andReturn();
        String rotated = objectMapper.readTree(refreshed.getResponse().getContentAsString())
                .get("refreshToken").asText();

        assertThat(rotated).isNotEqualTo(loginRefresh);
        assertThat(initialRefresh).isNotEqualTo(loginRefresh);

        // Re-using the already-rotated refresh token must fail: rotation revokes it.
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + loginRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        String email = "dup-" + System.nanoTime() + "@test.local";

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, STRONG_SECRET, "Dup User")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, STRONG_SECRET, "Dup User")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = "wrong-" + System.nanoTime() + "@test.local";

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email, STRONG_SECRET, "Wrong User")))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "NotTheSecret!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerValidationFailureReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"sh\",\"displayName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()")
                        .value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    private String registerBody(String email, String secret, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + secret
                + "\",\"displayName\":\"" + displayName + "\"}";
    }

    private String loginBody(String email, String secret) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + secret + "\"}";
    }
}
