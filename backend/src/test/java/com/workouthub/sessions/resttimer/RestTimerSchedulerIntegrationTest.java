package com.workouthub.sessions.resttimer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.workouthub.notifications.NotificationDispatcher;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class RestTimerSchedulerIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "RestTimerSecret1!";

    @Autowired RestTimerScheduleService service;
    @Autowired RestTimerScheduler scheduler;
    @Autowired RestTimerScheduleRepository repository;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired TestAuthHelpers helpers;

    @MockitoBean NotificationDispatcher dispatcher;

    @BeforeEach
    void resetDispatcher() {
        reset(dispatcher);
    }

    @Test
    void schedulerFiresOnceForADueRowAndMarksDispatchedAt() {
        SeededUser u = helpers.seed(
                "rt-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        // Schedule a 1-second timer, then back-date the row so it is due.
        RestTimerSchedule row = service.schedule(
                u.id(), sessionId, 1, "Rest over", "Next set", "/session/" + sessionId);
        row.setFireAt(Instant.now().minusSeconds(5));
        repository.save(row);

        scheduler.runForTests();

        verify(dispatcher, times(1))
                .send(
                        eq(u.id()),
                        eq("Rest over"),
                        eq("Next set"),
                        eq("/session/" + sessionId));

        Optional<RestTimerSchedule> after =
                repository.findByUserIdAndSessionId(u.id(), sessionId);
        assertThat(after).isPresent();
        assertThat(after.get().getDispatchedAt()).isNotNull();

        // A second tick must NOT re-fire - dispatched_at gates the row out.
        reset(dispatcher);
        scheduler.runForTests();
        verify(dispatcher, never())
                .send(eq(u.id()), anyString(), anyString(), anyString());
    }

    @Test
    void schedulerSkipsRowsThatAreNotDueYet() {
        SeededUser u = helpers.seed(
                "rt2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        // Future fire_at - should not be picked up.
        service.schedule(
                u.id(), sessionId, 600, "later", "later body", null);

        scheduler.runForTests();

        verify(dispatcher, never())
                .send(eq(u.id()), anyString(), anyString(), anyString());
    }

    @Test
    void cleanupRemovesDispatchedRowsOlderThanOneHour() {
        SeededUser u = helpers.seed(
                "rt3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        RestTimerSchedule row = service.schedule(
                u.id(), sessionId, 1, "t", "b", null);
        row.setDispatchedAt(Instant.now().minusSeconds(3600 + 60));
        repository.save(row);

        scheduler.runCleanupForTests();

        assertThat(repository.findByUserIdAndSessionId(u.id(), sessionId)).isEmpty();
    }

    @Test
    void cancelRemovesPendingRow() {
        SeededUser u = helpers.seed(
                "rt4-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        UUID sessionId = startSession(u.id());

        service.schedule(u.id(), sessionId, 60, "t", "b", null);
        assertThat(repository.findByUserIdAndSessionId(u.id(), sessionId)).isPresent();

        service.cancel(u.id(), sessionId);

        assertThat(repository.findByUserIdAndSessionId(u.id(), sessionId)).isEmpty();
    }

    private UUID startSession(UUID userId) {
        WorkoutSession s = new WorkoutSession();
        s.setUserId(userId);
        return sessions.save(s).getId();
    }
}
