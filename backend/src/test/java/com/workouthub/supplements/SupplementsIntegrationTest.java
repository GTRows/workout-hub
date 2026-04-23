package com.workouthub.supplements;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class SupplementsIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "SupplementSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void unauthenticatedCallReturns401() throws Exception {
        mvc.perform(get("/api/supplements")).andExpect(status().isUnauthorized());
    }

    @Test
    void postCreatesAndGetReturnsTheEntry() throws Exception {
        SeededUser u = helpers.seed(
                "sup1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Creatine","dosage":"5g","timing":"morning"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Creatine"))
                .andExpect(jsonPath("$.timing").value("morning"));

        mvc.perform(get("/api/supplements").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void putUpdatesTheEntry() throws Exception {
        SeededUser u = helpers.seed(
                "sup2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        MvcResult created = mvc.perform(post("/api/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Omega-3\",\"timing\":\"with_meal\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(put("/api/supplements/" + id)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dosage\":\"2g\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dosage").value("2g"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deleteRemovesTheEntry() throws Exception {
        SeededUser u = helpers.seed(
                "sup3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        MvcResult created = mvc.perform(post("/api/supplements")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"D3\",\"timing\":\"morning\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(delete("/api/supplements/" + id).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/supplements").header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidTimingReturns400() throws Exception {
        SeededUser u = helpers.seed(
                "sup4-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/supplements")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"?\",\"timing\":\"not_a_valid_value\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crossUserListIsIsolated() throws Exception {
        SeededUser a = helpers.seed(
                "supa-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser b = helpers.seed(
                "supb-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/supplements")
                        .header("Authorization", "Bearer " + a.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Creatine\",\"timing\":\"morning\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/supplements")
                        .header("Authorization", "Bearer " + b.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
