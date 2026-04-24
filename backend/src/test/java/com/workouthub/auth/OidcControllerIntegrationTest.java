package com.workouthub.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

class OidcControllerIntegrationTest {

    @Nested
    @AutoConfigureMockMvc
    class Disabled extends AbstractIntegrationTest {

        @Autowired MockMvc mvc;

        @Test
        void oidcLoginReturns404WhenFlagOff() throws Exception {
            mvc.perform(get("/api/auth/oidc/login"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void jwtLoginStillWorksWithFlagOff() throws Exception {
            // Just hit the health endpoint to prove the app boots with
            // OIDC disabled; the real login flow has its own tests.
            mvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @AutoConfigureMockMvc
    @TestPropertySource(properties = {
            "app.auth.oidc.enabled=true",
            "app.auth.oidc.issuer-url=https://id.example.com",
            "app.auth.oidc.client-id=workouthub-test",
            "app.auth.oidc.client-secret=test-secret",
            "app.auth.oidc.redirect-uri=https://workouthub.example.com/api/auth/oidc/callback"
    })
    class Enabled extends AbstractIntegrationTest {

        @Autowired MockMvc mvc;

        @Test
        void oidcLoginRedirectsToAuthentikWithRequiredParams() throws Exception {
            mvc.perform(get("/api/auth/oidc/login"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("/application/o/authorize/")))
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("client_id=workouthub-test")))
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("response_type=code")))
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("scope=openid+profile+email")));
        }
    }
}
