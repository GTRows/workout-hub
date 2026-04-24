package com.workouthub.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.workouthub.auth.domain.LoginAttempt;
import com.workouthub.auth.domain.LoginAttemptRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.users.domain.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class BruteForceLockoutIntegrationTest extends AbstractIntegrationTest {

    private static final String REAL_SECRET = "LockoutSecret1!";
    private static final String BAD_SECRET = "WrongPhrase1!";

    @Autowired MockMvc mvc;
    @Autowired TestAuthHelpers helpers;
    @Autowired LoginAttemptRepository attempts;

    @Test
    void underThresholdAllowsGoodPasswordToPass() throws Exception {
        String email = "bf-a-" + System.nanoTime() + "@test.local";
        helpers.seed(email, REAL_SECRET, Role.USER);

        for (int i = 0; i < 9; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(email, BAD_SECRET)))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, REAL_SECRET)))
                .andExpect(status().isOk());
    }

    @Test
    void hittingTheThresholdLocksWithHttp423() throws Exception {
        String email = "bf-b-" + System.nanoTime() + "@test.local";
        helpers.seed(email, REAL_SECRET, Role.USER);

        for (int i = 0; i < BruteForceGuard.MAX_FAILURES; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(email, BAD_SECRET)))
                    .andExpect(status().isUnauthorized());
        }

        // Good password, still locked.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, REAL_SECRET)))
                .andExpect(status().isLocked());
    }

    @Test
    void afterCooldownPassesOldFailuresAreIgnored() throws Exception {
        String email = "bf-c-" + System.nanoTime() + "@test.local";
        helpers.seed(email, REAL_SECRET, Role.USER);

        // Seed 10 failures but age them past the lockout window.
        Instant longAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        for (int i = 0; i < BruteForceGuard.MAX_FAILURES; i++) {
            LoginAttempt row = new LoginAttempt();
            row.setEmail(email);
            row.setSuccess(false);
            row.setAttemptedAt(longAgo);
            attempts.save(row);
        }

        // Stale failures are outside the window -> login succeeds.
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, REAL_SECRET)))
                .andExpect(status().isOk());
    }

    private static String body(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
