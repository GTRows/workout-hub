package com.workouthub.exports;

import com.workouthub.analytics.PrDetector;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.exports.dto.ClaudeSummaryDto;
import com.workouthub.exports.dto.ClaudeSummaryDto.BodyMetricSummary;
import com.workouthub.exports.dto.ClaudeSummaryDto.Consistency;
import com.workouthub.exports.dto.ClaudeSummaryDto.ExerciseEntry;
import com.workouthub.exports.dto.ClaudeSummaryDto.Period;
import com.workouthub.exports.dto.ClaudeSummaryDto.PrEntry;
import com.workouthub.exports.dto.ClaudeSummaryDto.SetEntry;
import com.workouthub.exports.dto.ClaudeSummaryDto.Totals;
import com.workouthub.exports.dto.ClaudeSummaryDto.UserSummary;
import com.workouthub.exports.dto.ClaudeSummaryDto.WorkoutEntry;
import com.workouthub.metrics.MetricsService;
import com.workouthub.metrics.dto.BodyMetricDto;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutFocus;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportService {

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final WorkoutSessionRepository sessions;
    private final WorkoutPlanRepository plans;
    private final MetricsService metrics;

    public ExportService(
            UserRepository users,
            UserProfileRepository profiles,
            WorkoutSessionRepository sessions,
            WorkoutPlanRepository plans,
            MetricsService metrics) {
        this.users = users;
        this.profiles = profiles;
        this.sessions = sessions;
        this.plans = plans;
        this.metrics = metrics;
    }

    public ClaudeSummaryDto buildSummary(UUID userId, int days) {
        int windowDays = Math.max(1, Math.min(days, 365));
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        UserProfile profile = profiles.findById(userId).orElse(null);

        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        LocalDate from = since.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate to = LocalDate.now(ZoneOffset.UTC);

        List<WorkoutSession> recent = sessions.findFinishedSince(userId, since);
        Optional<WorkoutPlan> activePlan = plans.findByUserIdAndActiveTrue(userId);
        Map<UUID, String> dayNames = collectDayNames(activePlan);
        List<BodyMetricDto> windowMetrics = metrics.list(userId, from, to);

        List<WorkoutEntry> entries = recent.stream()
                .map(session -> toEntry(session, dayNames))
                .toList();
        Totals totals = computeTotals(
                recent,
                computePlannedWorkouts(activePlan, from, to),
                windowMetrics);
        List<PrEntry> prs = computePrsInWindow(recent);
        List<BodyMetricSummary> bodyMetrics = computeBodyMetrics(windowMetrics);
        Consistency consistency = computeConsistency(recent, activePlan, from, to);

        return new ClaudeSummaryDto(
                new UserSummary(
                        user.getDisplayName(),
                        computeUserAge(profile, to),
                        profile == null ? null : profile.getHeightCm(),
                        profile == null ? null : profile.getWeightKg(),
                        profile == null ? null : profile.getHealthNotes(),
                        splitGoals(profile == null ? null : profile.getGoals())),
                new Period(from, to, windowDays),
                totals,
                entries,
                prs,
                bodyMetrics,
                consistency);
    }

    private static WorkoutEntry toEntry(WorkoutSession session, Map<UUID, String> dayNames) {
        LocalDate date = session.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
        Long durationMin = session.getEndedAt() == null
                ? null
                : Duration.between(session.getStartedAt(), session.getEndedAt()).toMinutes();
        String type = session.getWorkoutDayId() == null
                ? null
                : dayNames.get(session.getWorkoutDayId());

        // Preserve insertion order of exercises as encountered in the set list.
        Map<UUID, ExerciseGroup> grouped = new LinkedHashMap<>();
        for (SessionSet set : session.getSets()) {
            if (set.getExercise() == null) continue;
            UUID exId = set.getExercise().getId();
            grouped.computeIfAbsent(exId, k -> new ExerciseGroup(
                    set.getExercise().getNameTr(),
                    set.getExercise().getNameEn(),
                    new ArrayList<>()
            )).sets().add(new SetEntry(set.getWeightKg(), set.getRepsDone()));
        }
        List<ExerciseEntry> exercises = grouped.values().stream()
                .map(g -> new ExerciseEntry(g.nameTr(), g.nameEn(), g.sets()))
                .toList();

        return new WorkoutEntry(
                date,
                type,
                durationMin,
                exercises,
                session.getNotes(),
                session.getMood(),
                session.getEnergyLevel());
    }

    private static Totals computeTotals(
            List<WorkoutSession> sessions,
            Integer plannedWorkouts,
            List<BodyMetricDto> windowMetrics) {
        BigDecimal totalVolume = BigDecimal.ZERO;
        long totalDurationMin = 0;
        int durationSamples = 0;
        for (WorkoutSession session : sessions) {
            for (SessionSet set : session.getSets()) {
                BigDecimal w = set.getWeightKg() == null ? BigDecimal.ZERO : set.getWeightKg();
                totalVolume = totalVolume.add(w.multiply(BigDecimal.valueOf(set.getRepsDone())));
            }
            if (session.getEndedAt() != null) {
                totalDurationMin += Duration.between(
                        session.getStartedAt(), session.getEndedAt()).toMinutes();
                durationSamples++;
            }
        }
        Long avg = durationSamples == 0 ? null : totalDurationMin / durationSamples;
        int totalWorkouts = sessions.size();
        Integer adherence = (plannedWorkouts == null || plannedWorkouts == 0)
                ? null
                : (int) Math.round(totalWorkouts * 100.0 / plannedWorkouts);
        return new Totals(
                totalWorkouts,
                plannedWorkouts,
                adherence,
                totalVolume.setScale(2, RoundingMode.HALF_UP),
                avg,
                computeWeightChangeKg(windowMetrics));
    }

    private static Map<UUID, String> collectDayNames(Optional<WorkoutPlan> activePlan) {
        if (activePlan.isEmpty()) return Map.of();
        Map<UUID, String> out = new HashMap<>();
        for (WorkoutDay d : activePlan.get().getDays()) {
            out.put(d.getId(), d.getName());
        }
        return out;
    }

    private static Integer computeUserAge(UserProfile profile, LocalDate today) {
        if (profile == null || profile.getBirthDate() == null) return null;
        return java.time.Period.between(profile.getBirthDate(), today).getYears();
    }

    private static List<String> splitGoals(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split("\\r?\\n|;"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static Integer computePlannedWorkouts(
            Optional<WorkoutPlan> activePlan, LocalDate from, LocalDate to) {
        if (activePlan.isEmpty()) return null;
        Set<Short> activeDows = new HashSet<>();
        for (WorkoutDay d : activePlan.get().getDays()) {
            if (d.getFocus() == WorkoutFocus.REST) continue;
            activeDows.add(d.getDayOfWeek());
        }
        if (activeDows.isEmpty()) return 0;
        int count = 0;
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            short dow = (short) cursor.getDayOfWeek().getValue();
            if (activeDows.contains(dow)) count++;
            cursor = cursor.plusDays(1);
        }
        return count;
    }

    private static BigDecimal computeWeightChangeKg(List<BodyMetricDto> windowMetrics) {
        BigDecimal latest = null;
        BigDecimal earliest = null;
        for (BodyMetricDto m : windowMetrics) { // descending by recordedDate per MetricsService.list
            if (m.weightKg() == null) continue;
            if (latest == null) latest = m.weightKg();
            earliest = m.weightKg();
        }
        if (latest == null || earliest == null || latest.compareTo(earliest) == 0) return null;
        return latest.subtract(earliest).setScale(2, RoundingMode.HALF_UP);
    }

    private static List<BodyMetricSummary> computeBodyMetrics(List<BodyMetricDto> windowMetrics) {
        return windowMetrics.stream()
                .filter(m -> m.weightKg() != null)
                .map(m -> new BodyMetricSummary(m.recordedDate(), m.weightKg()))
                .toList();
    }

    private static List<PrEntry> computePrsInWindow(List<WorkoutSession> recent) {
        Map<UUID, PrAccumulator> best = new LinkedHashMap<>();
        for (WorkoutSession session : recent) {
            LocalDate sessionDate = session.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate();
            for (SessionSet set : session.getSets()) {
                if (set.getExercise() == null) continue;
                BigDecimal epley = PrDetector.epleyOneRm(set.getWeightKg(), set.getRepsDone());
                if (epley == null) continue;
                UUID exId = set.getExercise().getId();
                String exName = set.getExercise().getNameEn() != null
                        ? set.getExercise().getNameEn()
                        : set.getExercise().getNameTr();
                PrAccumulator current = best.get(exId);
                if (current == null || epley.compareTo(current.epley()) > 0) {
                    best.put(exId, new PrAccumulator(
                            exName, set.getWeightKg(), set.getRepsDone(), sessionDate, epley));
                }
            }
        }
        return best.values().stream()
                .sorted(Comparator.comparing(PrAccumulator::epley).reversed())
                .map(p -> new PrEntry(p.name(), p.weight(), p.reps(), p.date()))
                .toList();
    }

    private static Consistency computeConsistency(
            List<WorkoutSession> recent,
            Optional<WorkoutPlan> activePlan,
            LocalDate from,
            LocalDate to) {
        Set<LocalDate> sessionDates = new HashSet<>();
        for (WorkoutSession s : recent) {
            sessionDates.add(s.getStartedAt().atZone(ZoneOffset.UTC).toLocalDate());
        }
        int streak = 0;
        LocalDate cursor = to;
        while (!cursor.isBefore(from) && sessionDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        List<LocalDate> missed = new ArrayList<>();
        if (activePlan.isPresent()) {
            Set<Short> activeDows = new HashSet<>();
            for (WorkoutDay d : activePlan.get().getDays()) {
                if (d.getFocus() == WorkoutFocus.REST) continue;
                activeDows.add(d.getDayOfWeek());
            }
            LocalDate walker = from;
            while (!walker.isAfter(to)) {
                short dow = (short) walker.getDayOfWeek().getValue();
                if (activeDows.contains(dow) && !sessionDates.contains(walker)) {
                    missed.add(walker);
                }
                walker = walker.plusDays(1);
            }
        }

        return new Consistency(streak, missed, List.of());
    }

    private record ExerciseGroup(String nameTr, String nameEn, List<SetEntry> sets) {}

    private record PrAccumulator(
            String name,
            BigDecimal weight,
            short reps,
            LocalDate date,
            BigDecimal epley) {}
}
