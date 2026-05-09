package com.workouthub.sessions.resttimer;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Application-level operations for the rest-timer schedule lifecycle.
 *
 * <p>The service is the only writer to {@link RestTimerScheduleRepository}.
 * Cancellations are idempotent so a frontend that races two cancel calls
 * never breaks. The dispatch loop lives in {@link RestTimerScheduler}.
 */
@Service
@Transactional
public class RestTimerScheduleService {

    private static final int MIN_SECONDS = 1;
    private static final int MAX_SECONDS = 3600;
    private static final int MAX_TEXT_LENGTH = 500;

    private final RestTimerScheduleRepository repository;
    private final WorkoutSessionRepository sessions;
    private final Clock clock;

    public RestTimerScheduleService(
            RestTimerScheduleRepository repository,
            WorkoutSessionRepository sessions,
            Clock clock) {
        this.repository = repository;
        this.sessions = sessions;
        this.clock = clock;
    }

    public RestTimerSchedule schedule(
            @NotNull UUID userId,
            @NotNull UUID sessionId,
            int seconds,
            String title,
            String body,
            String clickUrl) {
        if (seconds < MIN_SECONDS || seconds > MAX_SECONDS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "seconds must be between " + MIN_SECONDS + " and " + MAX_SECONDS);
        }
        String safeTitle = requireText(title, "title");
        String safeBody = requireText(body, "body");
        String safeUrl = clickUrl == null || clickUrl.isBlank()
                ? "/session/" + sessionId
                : clickUrl;
        if (safeUrl.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "clickUrl too long");
        }

        sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Session not found: " + sessionId));

        Instant fireAt = Instant.now(clock).plusSeconds(seconds);
        RestTimerSchedule existing = repository
                .findByUserIdAndSessionId(userId, sessionId)
                .orElse(null);
        RestTimerSchedule row = existing != null ? existing : new RestTimerSchedule();
        row.setUserId(userId);
        row.setSessionId(sessionId);
        row.setFireAt(fireAt);
        row.setLocalizedTitle(safeTitle);
        row.setLocalizedBody(safeBody);
        row.setClickUrl(safeUrl);
        // Reset dispatched_at so a re-scheduled row is eligible to fire again.
        row.setDispatchedAt(null);
        return repository.save(row);
    }

    public void cancel(@NotNull UUID userId, @NotNull UUID sessionId) {
        repository.deleteByUserIdAndSessionId(userId, sessionId);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
        if (value.length() > MAX_TEXT_LENGTH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, field + " too long");
        }
        return value;
    }
}
