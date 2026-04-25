package com.workouthub.analytics;

import com.workouthub.analytics.dto.HeatmapDayDto;
import com.workouthub.analytics.dto.OneRmPointDto;
import com.workouthub.analytics.dto.PrDto;
import com.workouthub.analytics.dto.StreakDto;
import com.workouthub.analytics.dto.WeeklyVolumeDto;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.SessionSetRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final BigDecimal EPLEY_REP_DENOM = BigDecimal.valueOf(30);

    private final WorkoutSessionRepository sessions;
    private final SessionSetRepository sets;
    private final ExerciseRepository exercises;
    private final UserProfileRepository profiles;
    private final Clock clock;

    public AnalyticsService(
            WorkoutSessionRepository sessions,
            SessionSetRepository sets,
            ExerciseRepository exercises,
            UserProfileRepository profiles,
            Clock clock) {
        this.sessions = sessions;
        this.sets = sets;
        this.exercises = exercises;
        this.profiles = profiles;
        this.clock = clock;
    }

    public List<WeeklyVolumeDto> weeklyVolume(UUID userId, int weeks) {
        LocalDate monday = mondayOf(LocalDate.now(clock));
        LocalDate firstWeek = monday.minusWeeks(weeks - 1L);
        Instant since = firstWeek.atStartOfDay(ZONE).toInstant();

        List<WorkoutSession> finished = sessions.findFinishedSince(userId, since);

        Map<LocalDate, BigDecimal> volumeByWeek = new TreeMap<>();
        Map<LocalDate, Integer> countByWeek = new TreeMap<>();
        for (int i = 0; i < weeks; i++) {
            LocalDate w = firstWeek.plusWeeks(i);
            volumeByWeek.put(w, BigDecimal.ZERO);
            countByWeek.put(w, 0);
        }

        for (WorkoutSession s : finished) {
            LocalDate week = mondayOf(LocalDate.ofInstant(s.getStartedAt(), ZONE));
            if (!volumeByWeek.containsKey(week)) continue;
            BigDecimal vol = BigDecimal.ZERO;
            for (SessionSet set : s.getSets()) {
                if (!set.isCompleted()) continue;
                BigDecimal w = set.getWeightKg() == null ? BigDecimal.ZERO : set.getWeightKg();
                vol = vol.add(w.multiply(BigDecimal.valueOf(set.getRepsDone())));
            }
            volumeByWeek.merge(week, vol, BigDecimal::add);
            countByWeek.merge(week, 1, Integer::sum);
        }

        List<WeeklyVolumeDto> out = new ArrayList<>(weeks);
        for (LocalDate week : volumeByWeek.keySet()) {
            out.add(new WeeklyVolumeDto(
                    week,
                    volumeByWeek.get(week).setScale(2, RoundingMode.HALF_UP),
                    countByWeek.get(week)));
        }
        return out;
    }

    public List<OneRmPointDto> oneRepMax(UUID userId, UUID exerciseId) {
        exercises.findById(exerciseId)
                .orElseThrow(() -> new NotFoundException("Exercise not found: " + exerciseId));

        List<SessionSet> history = sets.findHistoricalByUserAndExercise(userId, exerciseId);

        Map<UUID, List<SessionSet>> bySession = new LinkedHashMap<>();
        for (SessionSet s : history) {
            bySession.computeIfAbsent(s.getSession().getId(), k -> new ArrayList<>()).add(s);
        }

        List<OneRmPointDto> out = new ArrayList<>();
        for (List<SessionSet> group : bySession.values()) {
            SessionSet top = null;
            BigDecimal topOneRm = null;
            for (SessionSet s : group) {
                BigDecimal oneRm = epley(s.getWeightKg(), s.getRepsDone());
                if (oneRm == null) continue;
                if (topOneRm == null || oneRm.compareTo(topOneRm) > 0) {
                    top = s;
                    topOneRm = oneRm;
                }
            }
            if (top == null) continue;
            LocalDate date = LocalDate.ofInstant(top.getSession().getStartedAt(), ZONE);
            out.add(new OneRmPointDto(
                    date,
                    topOneRm.setScale(2, RoundingMode.HALF_UP),
                    top.getRepsDone(),
                    top.getWeightKg()));
        }
        out.sort(Comparator.comparing(OneRmPointDto::date));
        return out;
    }

    @Transactional
    public StreakDto streak(UUID userId) {
        List<WorkoutSession> finished = sessions.findFinishedSince(userId, Instant.EPOCH);
        if (finished.isEmpty()) {
            return new StreakDto(0, 0, null);
        }

        List<LocalDate> dates = finished.stream()
                .map(s -> LocalDate.ofInstant(s.getStartedAt(), ZONE))
                .distinct()
                .sorted()
                .toList();

        LocalDate today = LocalDate.now(clock);
        UserProfile profile = profiles.findById(userId).orElse(null);
        String currentMonth = YearMonth.from(today).toString();
        boolean freezeAvailable = profile == null
                || profile.getStreakFreezeUsedMonth() == null
                || !profile.getStreakFreezeUsedMonth().equals(currentMonth);

        StreakCalculator.FreezeAwareResult r =
                StreakCalculator.computeWithFreeze(dates, today, freezeAvailable);

        if (freezeAvailable && r.freezeUsedInRun() && profile != null) {
            profile.setStreakFreezeUsedMonth(currentMonth);
        }
        return new StreakDto(r.current(), r.longest(), dates.get(dates.size() - 1));
    }

    public List<PrDto> personalRecords(UUID userId) {
        List<WorkoutSession> finished = sessions.findFinishedSince(userId, Instant.EPOCH);

        Map<UUID, SessionSet> bestByExercise = new HashMap<>();
        Map<UUID, BigDecimal> bestOneRm = new HashMap<>();
        for (WorkoutSession s : finished) {
            for (SessionSet set : s.getSets()) {
                if (!set.isCompleted()) continue;
                BigDecimal oneRm = epley(set.getWeightKg(), set.getRepsDone());
                if (oneRm == null) continue;
                UUID exId = set.getExercise().getId();
                BigDecimal prev = bestOneRm.get(exId);
                if (prev == null || oneRm.compareTo(prev) > 0) {
                    bestOneRm.put(exId, oneRm);
                    bestByExercise.put(exId, set);
                }
            }
        }

        List<PrDto> out = new ArrayList<>();
        for (Map.Entry<UUID, SessionSet> e : bestByExercise.entrySet()) {
            SessionSet set = e.getValue();
            Exercise ex = set.getExercise();
            LocalDate date = LocalDate.ofInstant(set.getSession().getStartedAt(), ZONE);
            out.add(new PrDto(
                    ex.getId(),
                    ex.getNameTr(),
                    ex.getNameEn(),
                    bestOneRm.get(e.getKey()).setScale(2, RoundingMode.HALF_UP),
                    set.getWeightKg(),
                    set.getRepsDone(),
                    date));
        }
        out.sort(Comparator.comparing(PrDto::estimatedOneRmKg).reversed());
        return out;
    }

    public List<HeatmapDayDto> heatmap(UUID userId, int weeks) {
        LocalDate monday = mondayOf(LocalDate.now(clock));
        LocalDate firstDay = monday.minusWeeks(weeks - 1L);
        Instant since = firstDay.atStartOfDay(ZONE).toInstant();

        List<WorkoutSession> finished = sessions.findFinishedSince(userId, since);

        Map<LocalDate, Integer> counts = new TreeMap<>();
        LocalDate lastDay = monday.plusDays(6);
        for (LocalDate d = firstDay; !d.isAfter(lastDay); d = d.plusDays(1)) {
            counts.put(d, 0);
        }
        for (WorkoutSession s : finished) {
            LocalDate d = LocalDate.ofInstant(s.getStartedAt(), ZONE);
            if (counts.containsKey(d)) counts.merge(d, 1, Integer::sum);
        }

        List<HeatmapDayDto> out = new ArrayList<>(counts.size());
        for (Map.Entry<LocalDate, Integer> e : counts.entrySet()) {
            out.add(new HeatmapDayDto(e.getKey(), e.getValue()));
        }
        return out;
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static BigDecimal epley(BigDecimal weightKg, short reps) {
        if (weightKg == null || reps <= 0) return null;
        if (reps == 1) return weightKg;
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(reps).divide(EPLEY_REP_DENOM, 6, RoundingMode.HALF_UP));
        return weightKg.multiply(factor);
    }
}
