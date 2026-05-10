package com.workouthub.sessions.resttimer;

import com.workouthub.notifications.NotificationDispatcher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Polls {@link RestTimerScheduleRepository} for due rows and dispatches a
 * push notification per row through the project-wide
 * {@link NotificationDispatcher}.
 *
 * <p>The dispatch path marks the row as {@code dispatched_at = now()} BEFORE
 * the network call. A failed Web Push send must not roll back the marker -
 * if it did, the next poll tick would re-fire the same notification. Pushes
 * are idempotent only per-tag at the SW layer, so an unbounded retry budget
 * here would surface as duplicate banners on the device.
 *
 * <p>A second {@code @Scheduled} tick prunes rows whose
 * {@code dispatched_at} is older than 1 hour, keeping the table bounded.
 */
@Component
public class RestTimerScheduler {

    private static final Logger log = LoggerFactory.getLogger(RestTimerScheduler.class);

    private static final int BATCH_SIZE = 50;
    private static final Duration CLEANUP_AGE = Duration.ofHours(1);

    private final RestTimerScheduleRepository repository;
    private final NotificationDispatcher dispatcher;
    private final Clock clock;

    public RestTimerScheduler(
            RestTimerScheduleRepository repository,
            NotificationDispatcher dispatcher,
            Clock clock) {
        this.repository = repository;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    /**
     * Marks due rows as dispatched in a single transaction, then fires
     * notifications outside the transaction so dispatcher latency does not
     * extend the row-lock window.
     */
    @Scheduled(fixedDelayString = "${app.rest-timer.poll-interval-ms:1000}")
    public void runOnce() {
        List<RestTimerSchedule> due = claimDueBatch();
        if (due.isEmpty()) return;
        for (RestTimerSchedule row : due) {
            try {
                dispatcher.send(
                        row.getUserId(),
                        row.getLocalizedTitle(),
                        row.getLocalizedBody(),
                        row.getClickUrl());
            } catch (RuntimeException ex) {
                log.warn(
                        "rest-timer dispatch failed userId={} sessionId={} scheduleId={}",
                        row.getUserId(), row.getSessionId(), row.getId(), ex);
            }
        }
        log.info("rest-timer dispatch tick processed={}", due.size());
    }

    // Self-call (runOnce -> claimDueBatch) bypasses Spring's transactional
    // proxy, so the @Transactional annotation alone never opened a tx and
    // dispatchedAt never persisted under Hibernate 7's stricter flushing.
    // Use Spring Data's saveAll to commit the dispatchedAt update explicitly
    // before the dispatch loop runs.
    protected List<RestTimerSchedule> claimDueBatch() {
        Instant now = Instant.now(clock);
        List<RestTimerSchedule> due = repository.findDueBatch(now, PageRequest.of(0, BATCH_SIZE));
        for (RestTimerSchedule row : due) {
            row.setDispatchedAt(now);
        }
        return repository.saveAll(due);
    }

    @Scheduled(fixedDelayString = "${app.rest-timer.cleanup-interval-ms:300000}")
    @Transactional
    public void runCleanup() {
        Instant cutoff = Instant.now(clock).minus(CLEANUP_AGE);
        int removed = repository.deleteDispatchedBefore(cutoff);
        if (removed > 0) {
            log.info("rest-timer cleanup removed={}", removed);
        }
    }

    // Visible-for-tests accessor; lets integration tests trigger one tick
    // synchronously without waiting on the @Scheduled cadence.
    public void runForTests() {
        runOnce();
    }

    public void runCleanupForTests() {
        runCleanup();
    }
}
