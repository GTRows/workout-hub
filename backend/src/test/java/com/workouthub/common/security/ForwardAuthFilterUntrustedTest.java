package com.workouthub.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies that when the request source IP is NOT in the configured
 * `app.auth.trusted-proxies` list, the X-Forwarded-* headers are ignored
 * and the request stays unauthenticated. Lives in its own class so it can
 * carry a different `trusted-proxies` value than its sibling.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.auth.mode=forward-auth",
        "app.auth.trusted-proxies=10.99.99.99/32"
})
class ForwardAuthFilterUntrustedTest extends AbstractIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;

    @Test
    void untrustedProxyIgnoresForwardedHeader() throws Exception {
        SeededUser u = helpers.seed(
                "uw-" + System.nanoTime() + "@test.local",
                "Str0ngPass!",
                Role.USER);
        mvc.perform(get("/api/users/me")
                        .header("X-Forwarded-Email", u.email()))
                .andExpect(status().isUnauthorized());
    }
}
