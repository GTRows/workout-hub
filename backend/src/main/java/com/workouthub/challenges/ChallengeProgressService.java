package com.workouthub.challenges;

import com.workouthub.analytics.StreakCalculator;
import com.workouthub.challenges.domain.MonthlyChallenge;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChallengeProgressService {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final WorkoutSessionRepository sessions;
    private final Clock clock;

    public ChallengeProgressService(
            WorkoutSessionRepository sessions,
            Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    public int compute(MonthlyChallenge challenge, UUID userId) {
        YearMonth ym = YearMonth.parse(challenge.getYearMonth());
        LocalDate first = ym.atDay(1);
        LocalDate lastInclusive = ym.atEndOfMonth();
        var monthStart = first.atStartOfDay(ZONE).toInstant();

        List<WorkoutSession> finished = sessions.findFinishedSince(userId, monthStart);
        finished = finished.stream()
                .filter(s -> {
                    LocalDate d = LocalDate.ofInstant(s.getStartedAt(), ZONE);
                    return !d.isBefore(first) && !d.isAfter(lastInclusive);
                })
                .toList();

        return switch (challenge.getRuleType()) {
            case "session_count" -> finished.size();
            case "volume_kg" -> totalVolume(finished);
            case "streak_days" -> currentStreakInMonth(finished, ym);
            default -> 0;
        };
    }

    private static int totalVolume(List<WorkoutSession> finished) {
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

    private int currentStreakInMonth(List<WorkoutSession> finished, YearMonth ym) {
        if (finished.isEmpty()) return 0;
        List<LocalDate> dates = finished.stream()
                .map(s -> LocalDate.ofInstant(s.getStartedAt(), ZONE))
                .distinct()
                .sorted()
                .toList();
        LocalDate today = LocalDate.now(clock);
        LocalDate cutoff = ym.atEndOfMonth();
        if (today.isAfter(cutoff)) today = cutoff;
        return StreakCalculator.compute(dates, today).current();
    }
}
