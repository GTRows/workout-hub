package com.workouthub.notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutFocus;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(ReminderJobIntegrationTest.TestConfig.class)
class ReminderJobIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "ReminderSecret1!";

    @Autowired ReminderJob job;
    @Autowired TestAuthHelpers helpers;
    @Autowired WorkoutPlanRepository plans;
    @Autowired BodyMetricRepository metrics;

    @MockitoBean NotificationDispatcher dispatcher;

    @BeforeEach
    void resetDispatcher() {
        reset(dispatcher);
    }

    @Test
    void workoutDayReminderFiresForUsersWithAnActivePlanDayOnThatDay() {
        SeededUser u = helpers.seed(
                "wd-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        seedActivePlanOn(u.id(), DayOfWeek.TUESDAY);

        // Simulate "today is Tuesday" via a known date.
        LocalDate aTuesday = LocalDate.of(2026, 4, 21);
        int count = job.runWorkoutDayRemindersAt(aTuesday);

        assertThat(count).isEqualTo(1);
        verify(dispatcher, atLeastOnce())
                .send(eq(u.id()), anyString(), anyString(), eq("/dashboard"));
    }

    @Test
    void workoutDayReminderDoesNotFireIfThePlanHasNoDayForToday() {
        SeededUser u = helpers.seed(
                "nd-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        seedActivePlanOn(u.id(), DayOfWeek.MONDAY);

        // Today is Saturday; no plan-day for Saturday -> no fire.
        LocalDate aSaturday = LocalDate.of(2026, 4, 25);
        int count = job.runWorkoutDayRemindersAt(aSaturday);

        assertThat(count).isZero();
        verify(dispatcher, never())
                .send(eq(u.id()), anyString(), anyString(), anyString());
    }

    @Test
    void weightNudgeFiresForUsersWithoutARecentBodyMetricsEntry() {
        SeededUser idle = helpers.seed(
                "idle-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        SeededUser recent = helpers.seed(
                "rec-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        // Recent user has an entry from 3 days ago -> skipped.
        LocalDate today = LocalDate.of(2026, 4, 23);
        BodyMetric m = new BodyMetric();
        m.setUserId(recent.id());
        m.setRecordedDate(today.minusDays(3));
        m.setWeightKg(new BigDecimal("80"));
        metrics.save(m);

        int count = job.runWeightNudgeAt(today);

        // Both users exist, only the idle one should be notified.
        assertThat(count).isGreaterThanOrEqualTo(1);
        verify(dispatcher, atLeastOnce())
                .send(eq(idle.id()), anyString(), anyString(), eq("/metrics"));
        verify(dispatcher, never())
                .send(eq(recent.id()), anyString(), anyString(), anyString());
    }

    private void seedActivePlanOn(UUID userId, DayOfWeek dow) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setName("Plan " + System.nanoTime());
        plan.setActive(true);

        WorkoutDay day = new WorkoutDay();
        day.setDayOfWeek((short) dow.getValue());
        day.setName("Day " + dow);
        day.setFocus(WorkoutFocus.PUSH);
        plan.addDay(day);

        plans.save(plan);
    }

    @TestConfiguration
    static class TestConfig {

        @org.springframework.context.annotation.Bean
        @Primary
        java.time.Clock fixedClock() {
            return java.time.Clock.fixed(
                    java.time.Instant.parse("2026-04-23T09:00:00Z"),
                    java.time.ZoneId.of("UTC"));
        }
    }

}
