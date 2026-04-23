package com.workouthub.notifications;

import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReminderJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderJob.class);
    private static final int WEIGHT_NUDGE_WINDOW_DAYS = 7;

    private final NotificationDispatcher dispatcher;
    private final WorkoutPlanRepository plans;
    private final BodyMetricRepository metrics;
    private final Clock clock;

    public ReminderJob(
            NotificationDispatcher dispatcher,
            WorkoutPlanRepository plans,
            BodyMetricRepository metrics,
            Clock clock) {
        this.dispatcher = dispatcher;
        this.plans = plans;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.reminders.workout-cron:-}")
    @Transactional(readOnly = true)
    public void runWorkoutDayReminders() {
        runWorkoutDayRemindersAt(LocalDate.now(clock));
    }

    @Scheduled(cron = "${app.reminders.weight-cron:-}")
    @Transactional(readOnly = true)
    public void runWeightNudge() {
        runWeightNudgeAt(LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public int runWorkoutDayRemindersAt(LocalDate today) {
        short dow = (short) today.getDayOfWeek().getValue();
        List<UUID> userIds = plans.findUserIdsWithActiveWorkoutOnDay(dow);
        for (UUID userId : userIds) {
            dispatcher.send(
                    userId,
                    "Today's workout is ready",
                    "Your plan has a workout scheduled for today.",
                    "/dashboard");
        }
        log.info("workout-day reminder fired userCount={} dayOfWeek={}", userIds.size(), dow);
        return userIds.size();
    }

    @Transactional(readOnly = true)
    public int runWeightNudgeAt(LocalDate today) {
        LocalDate since = today.minusDays(WEIGHT_NUDGE_WINDOW_DAYS);
        List<UUID> userIds = metrics.findUserIdsWithoutMetricsSince(since);
        for (UUID userId : userIds) {
            dispatcher.send(
                    userId,
                    "Log your weight",
                    "You have not logged body metrics in over 7 days.",
                    "/metrics");
        }
        log.info("weight-nudge fired userCount={} since={}", userIds.size(), since);
        return userIds.size();
    }
}
