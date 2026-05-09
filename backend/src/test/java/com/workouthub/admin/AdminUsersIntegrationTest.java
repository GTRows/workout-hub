package com.workouthub.admin;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class AdminUsersIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_SECRET = "AdminSecret!";
    private static final String USER_SECRET = "UserSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired ObjectMapper objectMapper;

    @Test
    void unauthenticatedCallerIsRejected() throws Exception {
        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void plainUserIsForbidden() throws Exception {
        SeededUser user = helpers.seed(
                "plain-" + System.nanoTime() + "@test.local", USER_SECRET, Role.USER);

        mvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + user.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateListUpdateAndDeleteUsers() throws Exception {
        SeededUser admin = helpers.seed(
                "admin-" + System.nanoTime() + "@test.local", ADMIN_SECRET, Role.ADMIN);
        String adminAuth = "Bearer " + admin.accessToken();

        String newEmail = "new-" + System.nanoTime() + "@test.local";
        String createBody = createBody(newEmail, "NewUserPass1!", "New User", Role.USER);

        MvcResult created = mvc.perform(post("/api/admin/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.role").value("USER"))
                .andReturn();
        UUID newUserId = UUID.fromString(objectMapper.readTree(
                created.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/admin/users").header("Authorization", adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));

        mvc.perform(put("/api/admin/users/" + newUserId)
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mvc.perform(delete("/api/admin/users/" + newUserId).header("Authorization", adminAuth))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/admin/users/" + newUserId).header("Authorization", adminAuth))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        SeededUser admin = helpers.seed(
                "dupadmin-" + System.nanoTime() + "@test.local", ADMIN_SECRET, Role.ADMIN);
        String adminAuth = "Bearer " + admin.accessToken();

        String sharedEmail = "shared-" + System.nanoTime() + "@test.local";
        mvc.perform(post("/api/admin/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(sharedEmail, "AnotherPass1!", "First", Role.USER)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/admin/users")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(sharedEmail, "AnotherPass1!", "Second", Role.USER)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCannotDeleteOwnAccount() throws Exception {
        SeededUser admin = helpers.seed(
                "self-" + System.nanoTime() + "@test.local", ADMIN_SECRET, Role.ADMIN);

        mvc.perform(delete("/api/admin/users/" + admin.id())
                        .header("Authorization", "Bearer " + admin.accessToken()))
                .andExpect(status().isConflict());
    }

    private String createBody(String email, String secret, String displayName, Role role) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + secret
                + "\",\"displayName\":\"" + displayName
                + "\",\"role\":\"" + role.name() + "\"}";
    }
}
