package com.workouthub.sessions.resttimer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class RestTimerScheduleServiceTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-05-09T10:00:00Z");

    private RestTimerScheduleRepository repository;
    private WorkoutSessionRepository sessions;
    private RestTimerScheduleService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(RestTimerScheduleRepository.class);
        sessions = Mockito.mock(WorkoutSessionRepository.class);
        Clock clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new RestTimerScheduleService(repository, sessions, clock);
    }

    @Test
    void scheduleInsertsRowWhenNonePending() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession owned = new WorkoutSession();
        owned.setUserId(userId);
        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(owned));
        when(repository.findByUserIdAndSessionId(userId, sessionId)).thenReturn(Optional.empty());
        when(repository.save(any(RestTimerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        RestTimerSchedule saved = service.schedule(
                userId, sessionId, 60, "Rest over", "60s rest complete", null);

        ArgumentCaptor<RestTimerSchedule> captor =
                ArgumentCaptor.forClass(RestTimerSchedule.class);
        verify(repository).save(captor.capture());
        RestTimerSchedule row = captor.getValue();
        assertThat(row.getUserId()).isEqualTo(userId);
        assertThat(row.getSessionId()).isEqualTo(sessionId);
        assertThat(row.getFireAt()).isEqualTo(FIXED_NOW.plusSeconds(60));
        assertThat(row.getLocalizedTitle()).isEqualTo("Rest over");
        assertThat(row.getLocalizedBody()).isEqualTo("60s rest complete");
        assertThat(row.getClickUrl()).isEqualTo("/session/" + sessionId);
        assertThat(row.getDispatchedAt()).isNull();
        assertThat(saved).isSameAs(row);
    }

    @Test
    void scheduleUpsertsExistingRowAndResetsDispatchedAt() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession owned = new WorkoutSession();
        owned.setUserId(userId);
        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(owned));

        RestTimerSchedule existing = new RestTimerSchedule();
        existing.setUserId(userId);
        existing.setSessionId(sessionId);
        existing.setFireAt(FIXED_NOW.minusSeconds(30));
        existing.setDispatchedAt(FIXED_NOW.minusSeconds(20));
        when(repository.findByUserIdAndSessionId(userId, sessionId))
                .thenReturn(Optional.of(existing));
        when(repository.save(any(RestTimerSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        service.schedule(userId, sessionId, 90, "Rest over", "next set", "/session/x");

        verify(repository, times(1)).save(existing);
        assertThat(existing.getFireAt()).isEqualTo(FIXED_NOW.plusSeconds(90));
        assertThat(existing.getDispatchedAt()).isNull();
        assertThat(existing.getClickUrl()).isEqualTo("/session/x");
    }

    @Test
    void scheduleRejectsZeroSeconds() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        assertThatThrownBy(() ->
                service.schedule(userId, sessionId, 0, "t", "b", null))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void scheduleRejectsTooManySeconds() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        assertThatThrownBy(() ->
                service.schedule(userId, sessionId, 3601, "t", "b", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void scheduleRejectsBlankTitle() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        WorkoutSession owned = new WorkoutSession();
        owned.setUserId(userId);
        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.of(owned));

        assertThatThrownBy(() ->
                service.schedule(userId, sessionId, 30, " ", "b", null))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void scheduleThrowsWhenSessionNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(sessions.findByIdAndUserId(sessionId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.schedule(userId, sessionId, 60, "t", "b", null))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void cancelDelegatesToRepositoryDelete() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        when(repository.deleteByUserIdAndSessionId(userId, sessionId)).thenReturn(1);

        service.cancel(userId, sessionId);

        verify(repository).deleteByUserIdAndSessionId(userId, sessionId);
    }
}
