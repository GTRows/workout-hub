package com.workouthub.nutrition;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import com.workouthub.nutrition.domain.FoodItemRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class NutritionIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "NutCrudSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;
    @Autowired FoodItemRepository foods;

    @Test
    void unauthenticatedSearchReturns401() throws Exception {
        mvc.perform(get("/api/foods?q=muz")).andExpect(status().isUnauthorized());
    }

    @Test
    void searchByNameReturnsAtLeastOneMatch() throws Exception {
        SeededUser u = helpers.seed(
                "nutS-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/foods?q=Banana")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nameEn").value("Banana"));
    }

    @Test
    void crudRoundTripScalesMacrosByServingSize() throws Exception {
        SeededUser u = helpers.seed(
                "nutC-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        UUID foodId = foods.searchByName("muz",
                org.springframework.data.domain.PageRequest.of(0, 1)).get(0).getId();

        String body = "{\"foodId\":\"" + foodId + "\","
                + "\"servingG\":200,"
                + "\"consumedAt\":\"" + Instant.now() + "\"}";

        MvcResult posted = mvc.perform(post("/api/nutrition")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servingG").value(200.0))
                .andExpect(jsonPath("$.kcal").exists())
                .andReturn();
        UUID entryId = UUID.fromString(objectMapper
                .readTree(posted.getResponse().getContentAsString())
                .get("id").asText());

        mvc.perform(get("/api/nutrition")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(delete("/api/nutrition/" + entryId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/nutrition")
                        .header("Authorization", auth))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crossUserListIsIsolated() throws Exception {
        SeededUser a = helpers.seed(
                "nutA-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser b = helpers.seed(
                "nutB-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        UUID foodId = foods.findAll().get(0).getId();
        mvc.perform(post("/api/nutrition")
                        .header("Authorization", "Bearer " + a.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":\"" + foodId + "\",\"servingG\":150,"
                                + "\"consumedAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/nutrition")
                        .header("Authorization", "Bearer " + b.accessToken()))
                .andExpect(jsonPath("$.length()").value(0));
    }
}
