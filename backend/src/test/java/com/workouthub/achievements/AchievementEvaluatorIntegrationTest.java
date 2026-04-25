package com.workouthub.achievements;

import static org.assertj.core.api.Assertions.assertThat;

import com.workouthub.achievements.domain.AchievementRepository;
import com.workouthub.achievements.domain.UserAchievementRepository;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.SessionSetRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.support.AbstractIntegrationTest;
import com.workouthub.support.TestAuthHelpers;
import com.workouthub.support.TestAuthHelpers.SeededUser;
import com.workouthub.users.domain.Role;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Transactional
class AchievementEvaluatorIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "AchSecret1!";

    @Autowired AchievementEvaluator evaluator;
    @Autowired AchievementRepository achievements;
    @Autowired UserAchievementRepository unlocks;
    @Autowired WorkoutSessionRepository sessions;
    @Autowired SessionSetRepository sets;
    @Autowired ExerciseRepository exercises;
    @Autowired TestAuthHelpers helpers;

    @Test
    void unlocksFirstWorkoutOnFirstFinishedSession() {
        SeededUser u = helpers.seed(
                "ach-" + System.nanoTime() + "@test.local", SECRET, Role.USER);

        seedFinishedSession(u.id(), Instant.now().minusSeconds(3600));
        evaluator.onSessionFinished(u.id());

        assertThat(unlockedCodes(u.id())).contains("first-workout");
    }

    @Test
    void doesNotUnlockTwiceOnRepeatedEvaluations() {
        SeededUser u = helpers.seed(
                "ach2-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        seedFinishedSession(u.id(), Instant.now().minusSeconds(3600));

        evaluator.onSessionFinished(u.id());
        evaluator.onSessionFinished(u.id());
        evaluator.onSessionFinished(u.id());

        long firstWorkoutCount = unlocks.findByUserIdOrderByUnlockedAtDesc(u.id())
                .stream()
                .filter(x -> achievements.findById(x.getAchievementId())
                        .map(a -> "first-workout".equals(a.getCode()))
                        .orElse(false))
                .count();
        assertThat(firstWorkoutCount).isEqualTo(1L);
    }

    @Test
    void unlocksVolumeAchievementWhenTotalCrossesThreshold() {
        SeededUser u = helpers.seed(
                "ach3-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        Exercise ex = exercises.findAll().stream().findFirst().orElseThrow();

        WorkoutSession session = seedFinishedSession(u.id(), Instant.now().minusSeconds(3600));
        seedSet(session, ex, new BigDecimal("100.0"), (short) 12);
        seedSet(session, ex, new BigDecimal("100.0"), (short) 12);

        evaluator.onSetSaved(u.id(), false);

        assertThat(unlockedCodes(u.id())).contains("volume-1000");
    }

    @Test
    void doesNotUnlockBelowThreshold() {
        SeededUser u = helpers.seed(
                "ach4-" + System.nanoTime() + "@test.local", SECRET, Role.USER);
        Exercise ex = exercises.findAll().stream().findFirst().orElseThrow();

        WorkoutSession session = seedFinishedSession(u.id(), Instant.now().minusSeconds(3600));
        seedSet(session, ex, new BigDecimal("20.0"), (short) 5);

        evaluator.onSetSaved(u.id(), false);

        assertThat(unlockedCodes(u.id())).doesNotContain("volume-1000", "volume-10000");
    }

    private WorkoutSession seedFinishedSession(UUID userId, Instant startedAt) {
        WorkoutSession s = new WorkoutSession();
        s.setUserId(userId);
        s.setStartedAt(startedAt);
        s.setEndedAt(startedAt.plusSeconds(1800));
        return sessions.save(s);
    }

    private void seedSet(WorkoutSession session, Exercise ex, BigDecimal kg, short reps) {
        SessionSet set = new SessionSet();
        set.setExercise(ex);
        set.setSetNumber((short) (session.getSets().size() + 1));
        set.setRepsDone(reps);
        set.setWeightKg(kg);
        set.setCompleted(true);
        session.addSet(set);
        sets.save(set);
    }

    private java.util.Set<String> unlockedCodes(UUID userId) {
        return unlocks.findByUserIdOrderByUnlockedAtDesc(userId).stream()
                .map(ua -> achievements.findById(ua.getAchievementId())
                        .map(a -> a.getCode()).orElse(null))
                .filter(c -> c != null)
                .collect(java.util.stream.Collectors.toSet());
    }
}
