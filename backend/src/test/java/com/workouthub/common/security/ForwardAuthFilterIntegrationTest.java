package com.workouthub.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the trust gate of ForwardAuthFilter:
 *  - Trusted proxy IP + known forwarded email -> authenticates and 200.
 *  - Trusted proxy IP + unknown forwarded email -> stays unauthenticated, 401.
 *
 * The "untrusted proxy" scenario is covered in {@link ForwardAuthFilterUntrustedTest}
 * because it requires a different `app.auth.trusted-proxies` value.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.auth.mode=forward-auth",
        "app.auth.trusted-proxies=127.0.0.1/32"
})
class ForwardAuthFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void trustedProxyWithKnownEmailAuthenticates() throws Exception {
        SeededUser u = helpers.seed(
                "fwd-" + System.nanoTime() + "@test.local",
                "Str0ngPass!",
                Role.USER);
        mvc.perform(get("/api/users/me")
                        .header("X-Forwarded-Email", u.email()))
                .andExpect(status().isOk());
    }

    @Test
    void trustedProxyWithUnknownEmailReturns401() throws Exception {
        mvc.perform(get("/api/users/me")
                        .header("X-Forwarded-Email", "ghost-" + System.nanoTime() + "@nowhere.local"))
                .andExpect(status().isUnauthorized());
    }
}
