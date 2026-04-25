package com.workouthub.achievements;

import com.workouthub.achievements.domain.Achievement;
import com.workouthub.achievements.domain.AchievementRepository;
import com.workouthub.achievements.domain.UserAchievement;
import com.workouthub.achievements.domain.UserAchievementRepository;
import com.workouthub.analytics.StreakCalculator;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementEvaluator {

    static final String RULE_SESSION_COUNT = "session_count";
    static final String RULE_STREAK_DAYS = "streak_days";
    static final String RULE_PR_COUNT = "pr_count";
    static final String RULE_VOLUME_KG = "volume_kg";

    private static final Logger LOG = LoggerFactory.getLogger(AchievementEvaluator.class);
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final AchievementRepository achievements;
    private final UserAchievementRepository unlocks;
    private final WorkoutSessionRepository sessions;
    private final Clock clock;

    public AchievementEvaluator(
            AchievementRepository achievements,
            UserAchievementRepository unlocks,
            WorkoutSessionRepository sessions,
            Clock clock) {
        this.achievements = achievements;
        this.unlocks = unlocks;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public List<UserAchievement> onSessionFinished(UUID userId) {
        List<WorkoutSession> finished = sessions.findFinishedSince(userId, Instant.EPOCH);
        List<UserAchievement> newlyUnlocked = new ArrayList<>();
        unlockIfMet(userId, RULE_SESSION_COUNT, finished.size(), newlyUnlocked);

        if (!finished.isEmpty()) {
            List<LocalDate> dates = finished.stream()
                    .map(s -> LocalDate.ofInstant(s.getStartedAt(), ZONE))
                    .distinct()
                    .sorted()
                    .toList();
            StreakCalculator.Result r = StreakCalculator.compute(dates, LocalDate.now(clock));
            unlockIfMet(userId, RULE_STREAK_DAYS, r.current(), newlyUnlocked);
        }
        return newlyUnlocked;
    }

    @Transactional
    public List<UserAchievement> onSetSaved(UUID userId, boolean wasNewPr) {
        List<UserAchievement> newlyUnlocked = new ArrayList<>();
        if (wasNewPr) {
            int prCount = countDistinctPrExercises(userId);
            unlockIfMet(userId, RULE_PR_COUNT, prCount, newlyUnlocked);
        }
        int totalVolume = totalVolumeKg(userId);
        unlockIfMet(userId, RULE_VOLUME_KG, totalVolume, newlyUnlocked);
        return newlyUnlocked;
    }

    private void unlockIfMet(
            UUID userId,
            String ruleType,
            int currentValue,
            List<UserAchievement> sink) {
        for (Achievement def : achievements.findByRuleTypeOrderByThresholdAsc(ruleType)) {
            if (currentValue < def.getThreshold()) continue;
            if (unlocks.existsByUserIdAndAchievementId(userId, def.getId())) continue;
            UserAchievement ua = new UserAchievement();
            ua.setUserId(userId);
            ua.setAchievementId(def.getId());
            ua.setProgressValue(currentValue);
            try {
                sink.add(unlocks.save(ua));
            } catch (org.springframework.dao.DataIntegrityViolationException dup) {
                // Race: another transaction beat us. The unique constraint
                // already protects against double-unlock, so this is benign.
                LOG.debug("Achievement {} already unlocked for user {}", def.getCode(), userId);
            }
        }
    }

    private int countDistinctPrExercises(UUID userId) {
        List<WorkoutSession> finished = sessions.findFinishedSince(userId, Instant.EPOCH);
        java.util.Map<UUID, BigDecimal> bestOneRm = new java.util.HashMap<>();
        for (WorkoutSession s : finished) {
            for (SessionSet set : s.getSets()) {
                if (!set.isCompleted() || set.getWeightKg() == null) continue;
                BigDecimal oneRm = epley(set.getWeightKg(), set.getRepsDone());
                if (oneRm == null) continue;
                UUID exId = set.getExercise().getId();
                BigDecimal prev = bestOneRm.get(exId);
                if (prev == null || oneRm.compareTo(prev) > 0) {
                    bestOneRm.put(exId, oneRm);
                }
            }
        }
        return bestOneRm.size();
    }

    private int totalVolumeKg(UUID userId) {
        List<WorkoutSession> finished = sessions.findFinishedSince(userId, Instant.EPOCH);
        BigDecimal total = BigDecimal.ZERO;
        for (WorkoutSession s : finished) {
            for (SessionSet set : s.getSets()) {
                if (!set.isCompleted() || set.getWeightKg() == null) continue;
                total = total.add(set.getWeightKg().multiply(
                        BigDecimal.valueOf(set.getRepsDone())));
            }
        }
        return total.intValue();
    }

    private static BigDecimal epley(BigDecimal weightKg, short reps) {
        if (weightKg == null || reps <= 0) return null;
        if (reps == 1) return weightKg;
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(BigDecimal.valueOf(30), 6, java.math.RoundingMode.HALF_UP));
        return weightKg.multiply(factor);
    }

}
