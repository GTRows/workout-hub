package com.workouthub.webhooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import com.workouthub.webhooks.domain.WebhookToken;
import com.workouthub.webhooks.domain.WebhookTokenRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class ScaleWebhookIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "ScaleSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired WebhookTokenRepository tokenRepo;
    @Autowired BodyMetricRepository metricRepo;

    @Test
    void acceptsValidTokenAndWritesBodyMetric() throws Exception {
        SeededUser u = helpers.seed(
                "scale-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        MvcResult mintResult = mvc.perform(post("/api/users/me/webhook-tokens")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(mintResult.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(post("/api/webhooks/scale/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":81.4,\"timestamp\":\"2026-04-23T07:00:00Z\"}"))
                .andExpect(status().isCreated());

        assertThat(metricRepo.findByUserIdAndRecordedDate(
                u.id(), LocalDate.parse("2026-04-23")))
                .isPresent();
    }

    @Test
    void rejectsUnknownTokenWith401() throws Exception {
        mvc.perform(post("/api/webhooks/scale/totally-fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":81.4,\"timestamp\":\"2026-04-23T07:00:00Z\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void isIdempotentOnSameDate() throws Exception {
        SeededUser u = helpers.seed(
                "scale2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        MvcResult mintResult = mvc.perform(post("/api/users/me/webhook-tokens")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(mintResult.getResponse().getContentAsString())
                .get("token").asText();

        mvc.perform(post("/api/webhooks/scale/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":81.4,\"timestamp\":\"2026-04-23T07:00:00Z\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/webhooks/scale/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":80.5,\"timestamp\":\"2026-04-23T18:00:00Z\"}"))
                .andExpect(status().isOk());

        assertThat(metricRepo.findByUserIdAndRecordedDate(
                u.id(), LocalDate.parse("2026-04-23"))
                .orElseThrow().getWeightKg().toPlainString())
                .startsWith("81.4");
    }

    @Test
    void acceptsAliasFieldNamesFromHomeAssistant() throws Exception {
        SeededUser u = helpers.seed(
                "scale3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        MvcResult mintResult = mvc.perform(post("/api/users/me/webhook-tokens")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isCreated())
                .andReturn();
        WebhookToken token = tokenRepo.findByUserIdAndPurpose(
                u.id(), "scale").get(0);

        mvc.perform(post("/api/webhooks/scale/" + token.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":78.2,\"ts\":\"2026-04-22T07:00:00Z\"}"))
                .andExpect(status().isCreated());
    }
}
