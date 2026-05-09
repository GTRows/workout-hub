package com.workouthub.push;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class PushTestEndpointTest extends AbstractIntegrationTest {

    private static final String SECRET = "PushTestSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void zeroSubscriptionsReturnsZeroDeliveredAndZeroSubscriptions() throws Exception {
        SeededUser u = helpers.seed(
                "pt0-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/push/test")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(0))
                .andExpect(jsonPath("$.subscriptions").value(0));
    }

    @Test
    void oneSubscriptionReturnsOneDeliveredAndOneSubscription() throws Exception {
        SeededUser u = helpers.seed(
                "pt1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/push/subscribe")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "https://push.example/test-%d",
                                  "keys": { "p256dh": "kkk", "auth": "aaa" }
                                }
                                """.formatted(System.nanoTime())))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/push/test")
                        .header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(1))
                .andExpect(jsonPath("$.subscriptions").value(1));
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mvc.perform(post("/api/push/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responseShapeIsTwoIntegerFields() throws Exception {
        SeededUser u = helpers.seed(
                "ptShape-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/push/test")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").isNumber())
                .andExpect(jsonPath("$.subscriptions").isNumber());
    }
}
