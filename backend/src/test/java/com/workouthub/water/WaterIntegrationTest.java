package com.workouthub.water;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class WaterIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "WaterSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void postsTwoSipsAndGetSumsThemForToday() throws Exception {
        SeededUser u = helpers.seed(
                "w-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/water")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ml\":250,\"consumedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/water")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ml\":500,\"consumedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/water").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMl").value(750))
                .andExpect(jsonPath("$.entries.length()").value(2));
    }

    @Test
    void deletesASipAndShrinksTheTotal() throws Exception {
        SeededUser u = helpers.seed(
                "w2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        MvcResult posted = mvc.perform(post("/api/water")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ml\":250,\"consumedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper
                .readTree(posted.getResponse().getContentAsString())
                .get("id").asText());

        mvc.perform(delete("/api/water/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/water").header("Authorization", auth))
                .andExpect(jsonPath("$.totalMl").value(0));
    }

    @Test
    void rejectsAbsurdMlValuesWith400() throws Exception {
        SeededUser u = helpers.seed(
                "w3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/water")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ml\":50000,\"consumedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
