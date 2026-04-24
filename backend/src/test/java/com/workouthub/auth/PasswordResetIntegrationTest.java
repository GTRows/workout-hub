package com.workouthub.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.auth.domain.PasswordResetToken;
import com.workouthub.auth.domain.PasswordResetTokenRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class PasswordResetIntegrationTest extends AbstractIntegrationTest {

    private static final String OLD_SECRET = "ResetSecret1!";
    private static final String FIRST_SECRET = "FirstNewPhrase1!";
    private static final String SECOND_SECRET = "SecondNewPhrase1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordResetTokenRepository tokens;

    @Test
    void adminCanIssueResetLinkAndUserCanConsumeIt() throws Exception {
        SeededUser admin = helpers.seed(
                "admin-r-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.ADMIN);
        SeededUser target = helpers.seed(
                "tgt-r-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.USER);

        MvcResult r = mvc.perform(post("/api/admin/users/" + target.id() + "/reset-link")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(r.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\""
                                + FIRST_SECRET + "\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + target.email() + "\",\"password\":\""
                                + OLD_SECRET + "\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + target.email() + "\",\"password\":\""
                                + FIRST_SECRET + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void consumingSameTokenTwiceRejectsTheSecondCall() throws Exception {
        SeededUser admin = helpers.seed(
                "admin-x-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.ADMIN);
        SeededUser target = helpers.seed(
                "tgt-x-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.USER);

        MvcResult r = mvc.perform(post("/api/admin/users/" + target.id() + "/reset-link")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(r.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\""
                                + FIRST_SECRET + "\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\""
                                + SECOND_SECRET + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        SeededUser admin = helpers.seed(
                "admin-y-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.ADMIN);
        SeededUser target = helpers.seed(
                "tgt-y-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.USER);

        MvcResult r = mvc.perform(post("/api/admin/users/" + target.id() + "/reset-link")
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(r.getResponse().getContentAsString())
                .get("token").asText();

        PasswordResetToken row = tokens.findAll().stream()
                .filter(t -> t.getUserId().equals(target.id()))
                .findFirst().orElseThrow();
        row.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        tokens.save(row);

        mvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"newPassword\":\""
                                + FIRST_SECRET + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotIssueResetLinks() throws Exception {
        SeededUser notAdmin = helpers.seed(
                "naz-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.USER);
        SeededUser target = helpers.seed(
                "nat-" + System.nanoTime() + "@test.local", OLD_SECRET, Role.USER);

        mvc.perform(post("/api/admin/users/" + target.id() + "/reset-link")
                        .header("Authorization", "Bearer " + notAdmin.accessToken()))
                .andExpect(status().isForbidden());
    }
}
