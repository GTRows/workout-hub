package com.workouthub.audit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AuditLogIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "AuditSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createUpdateDeleteAreAllRecorded() throws Exception {
        SeededUser admin = helpers.seed(
                "adt-" + System.nanoTime() + "@test.local", SECRET, Role.ADMIN);
        String auth = "Bearer " + admin.accessToken();

        String createBody = "{\"email\":\"au-" + System.nanoTime()
                + "@test.local\",\"password\":\"MadeUpSecret1!\","
                + "\"displayName\":\"Audit Target\",\"role\":\"USER\"}";

        MvcResult created = mvc.perform(post("/api/admin/users")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        UUID targetId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("id").asText());

        mvc.perform(put("/api/admin/users/" + targetId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Renamed\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete("/api/admin/users/" + targetId)
                        .header("Authorization", auth))
                .andExpect(status().isNoContent());

        // Audit log should contain all three actions.
        mvc.perform(get("/api/admin/audit")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.action == 'user.create' && @.targetId == '"
                                + targetId + "')]")
                        .isNotEmpty())
                .andExpect(jsonPath(
                        "$[?(@.action == 'user.update' && @.targetId == '"
                                + targetId + "')]")
                        .isNotEmpty())
                .andExpect(jsonPath(
                        "$[?(@.action == 'user.delete' && @.targetId == '"
                                + targetId + "')]")
                        .isNotEmpty());
    }

    @Test
    void nonAdminCannotReadAudit() throws Exception {
        SeededUser user = helpers.seed(
                "nau-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/admin/audit")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isForbidden());
    }
}
