package com.workouthub.push;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.push.domain.PushSubscriptionRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.push.vapid.public-key=test-public",
        "app.push.vapid.private-key=test-private"
})
class PushIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "PushSecret1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired PushSubscriptionRepository repo;

    @Test
    void subscribeStoresTheEndpointAndReturnsItsId() throws Exception {
        SeededUser u = helpers.seed(
                "p1-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        mvc.perform(post("/api/push/subscribe")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "endpoint": "https://push.example/abc",
                                  "keys": { "p256dh": "keyAAAAAAA", "auth": "authBBBB" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endpoint").value("https://push.example/abc"));
    }

    @Test
    void secondSubscribeForSameEndpointIsIdempotent() throws Exception {
        SeededUser u = helpers.seed(
                "p2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();

        String endpoint = "https://push.example/unique-" + System.nanoTime();
        String body1 = """
                {
                  "endpoint": "%s",
                  "keys": { "p256dh": "keyOld", "auth": "authOld" }
                }
                """.formatted(endpoint);
        String body2 = """
                {
                  "endpoint": "%s",
                  "keys": { "p256dh": "keyNew", "auth": "authNew" }
                }
                """.formatted(endpoint);

        mvc.perform(post("/api/push/subscribe")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body1))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/push/subscribe")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isCreated());

        // Only one row should exist for that endpoint.
        long total = repo.findByEndpoint(endpoint).stream().count();
        org.assertj.core.api.Assertions.assertThat(total).isEqualTo(1);
    }

    @Test
    void unauthenticatedCallReturns401() throws Exception {
        mvc.perform(post("/api/push/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"x\",\"keys\":{\"p256dh\":\"a\",\"auth\":\"b\"}}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPayloadReturns400() throws Exception {
        SeededUser u = helpers.seed(
                "p3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(post("/api/push/subscribe")
                        .header("Authorization", "Bearer " + u.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"\",\"keys\":{\"p256dh\":\"\",\"auth\":\"\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vapidPublicKeyIsReturnedFromConfig() throws Exception {
        SeededUser u = helpers.seed(
                "p4-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        mvc.perform(get("/api/push/vapid-public-key")
                        .header("Authorization", "Bearer " + u.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").value("test-public"));
    }

    @Test
    void deleteRemovesTheSubscription() throws Exception {
        SeededUser u = helpers.seed(
                "p5-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        String auth = "Bearer " + u.accessToken();
        String endpoint = "https://push.example/del-" + System.nanoTime();

        mvc.perform(post("/api/push/subscribe")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"endpoint\":\"" + endpoint + "\","
                                + "\"keys\":{\"p256dh\":\"a\",\"auth\":\"b\"}}"))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/push/subscribe")
                        .header("Authorization", auth)
                        .param("endpoint", endpoint))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(repo.findByEndpoint(endpoint)).isEmpty();
    }
}
