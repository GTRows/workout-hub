package com.workouthub.auth;

import com.workouthub.auth.domain.LoginAttempt;
import com.workouthub.auth.domain.LoginAttemptRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 10 failed logins for the same email within the last 15 minutes locks
 * that email out for 1 hour counted from the most recent failure.
 */
@Component
@Transactional
public class BruteForceGuard {

    static final int MAX_FAILURES = 10;
    static final long WINDOW_MINUTES = 15;
    static final long LOCKOUT_MINUTES = 60;

    private final LoginAttemptRepository attempts;
    private final Clock clock;

    public BruteForceGuard(LoginAttemptRepository attempts, Clock clock) {
        this.attempts = attempts;
        this.clock = clock;
    }

    /** Throws HTTP 423 if the email is currently locked out. */
    public void assertNotLocked(String email) {
        Instant now = Instant.now(clock);
        long failures = attempts.countFailuresSince(
                email, now.minus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        if (failures >= MAX_FAILURES) {
            throw new ResponseStatusException(HttpStatus.LOCKED,
                    "Too many failed attempts. Try again later.");
        }
    }

    public void recordSuccess(String email) {
        record(email, true);
    }

    public void recordFailure(String email) {
        record(email, false);
    }

    private void record(String email, boolean success) {
        LoginAttempt row = new LoginAttempt();
        row.setEmail(email == null ? "" : email);
        row.setSuccess(success);
        row.setAttemptedAt(Instant.now(clock));
        attempts.save(row);
    }
}
