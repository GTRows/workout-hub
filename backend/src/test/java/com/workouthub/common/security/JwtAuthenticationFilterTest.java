package com.workouthub.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class JwtAuthenticationFilterTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtService jwtService;

    @Test
    void validAccessTokenAuthenticatesTheRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateAccessToken(userId, "USER");

        mvc.perform(get("/api/test-secured/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(userId.toString()));
    }

    @Test
    void refreshTokenCannotAccessResource() throws Exception {
        String refresh = jwtService.generateRefreshToken(UUID.randomUUID(), "USER");

        mvc.perform(get("/api/test-secured/me")
                        .header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejectedAt401() throws Exception {
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "USER");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        mvc.perform(get("/api/test-secured/me")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingAuthorizationHeaderReturns401() throws Exception {
        mvc.perform(get("/api/test-secured/me"))
                .andExpect(status().isUnauthorized());
    }
}
