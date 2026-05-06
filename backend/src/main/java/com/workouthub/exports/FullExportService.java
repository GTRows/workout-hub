package com.workouthub.exports;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.exports.dto.FullExportDto;
import com.workouthub.exports.dto.FullExportDto.DayExerciseRow;
import com.workouthub.exports.dto.FullExportDto.DayRow;
import com.workouthub.exports.dto.FullExportDto.MetricRow;
import com.workouthub.exports.dto.FullExportDto.PlanSection;
import com.workouthub.exports.dto.FullExportDto.SessionSection;
import com.workouthub.exports.dto.FullExportDto.SetRow;
import com.workouthub.exports.dto.FullExportDto.SupplementRow;
import com.workouthub.exports.dto.FullExportDto.UserSection;
import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.supplements.domain.Supplement;
import com.workouthub.supplements.domain.SupplementRepository;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutDayExercise;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FullExportService {

    private static final int SCHEMA_VERSION = 1;

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final WorkoutPlanRepository plans;
    private final WorkoutSessionRepository sessions;
    private final BodyMetricRepository metrics;
    private final SupplementRepository supplements;

    public FullExportService(
            UserRepository users,
            UserProfileRepository profiles,
            WorkoutPlanRepository plans,
            WorkoutSessionRepository sessions,
            BodyMetricRepository metrics,
            SupplementRepository supplements) {
        this.users = users;
        this.profiles = profiles;
        this.plans = plans;
        this.sessions = sessions;
        this.metrics = metrics;
        this.supplements = supplements;
    }

    public FullExportDto build(UUID userId) {
        return new FullExportDto(
                SCHEMA_VERSION,
                Instant.now(),
                buildUserSection(userId),
                buildPlans(userId),
                buildSessions(userId),
                buildMetrics(userId),
                buildSupplements(userId));
    }

    public UserSection buildUserSection(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        UserProfile profile = profiles.findById(userId).orElse(null);
        return new UserSection(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                profile == null ? null : profile.getHeightCm(),
                profile == null ? null : profile.getWeightKg(),
                profile == null ? null : profile.getBirthDate(),
                profile == null ? null : profile.getGender(),
                profile == null ? null : profile.getHealthNotes(),
                profile == null ? null : profile.getGoals());
    }

    public List<PlanSection> buildPlans(UUID userId) {
        return plans.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(FullExportService::toPlanSection)
                .toList();
    }

    public List<SessionSection> buildSessions(UUID userId) {
        return sessions
                .findByUserIdAndEndedAtIsNotNullOrderByStartedAtDesc(userId).stream()
                .map(FullExportService::toSessionSection)
                .toList();
    }

    public List<MetricRow> buildMetrics(UUID userId) {
        return metrics.findByUserIdOrderByRecordedDateDesc(userId).stream()
                .map(FullExportService::toMetricRow)
                .toList();
    }

    public List<SupplementRow> buildSupplements(UUID userId) {
        return supplements.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(FullExportService::toSupplementRow)
                .toList();
    }

    private static PlanSection toPlanSection(WorkoutPlan p) {
        List<DayRow> days = p.getDays().stream()
                .map(FullExportService::toDayRow)
                .toList();
        return new PlanSection(p.getId(), p.getName(), p.isActive(), days);
    }

    private static DayRow toDayRow(WorkoutDay d) {
        List<DayExerciseRow> items = d.getExercises().stream()
                .map(FullExportService::toDayExerciseRow)
                .toList();
        return new DayRow(
                d.getId(),
                d.getDayOfWeek(),
                d.getName(),
                d.getFocus() == null ? null : d.getFocus().name().toLowerCase(),
                d.getEstimatedDurationMin(),
                items);
    }

    private static DayExerciseRow toDayExerciseRow(WorkoutDayExercise e) {
        return new DayExerciseRow(
                e.getId(),
                e.getExercise() == null ? null : e.getExercise().getId(),
                e.getOrderIndex(),
                e.getTargetSets(),
                e.getTargetRepsMin(),
                e.getTargetRepsMax(),
                e.getTargetWeightKg(),
                e.getRestSeconds(),
                e.getNotes());
    }

    private static SessionSection toSessionSection(WorkoutSession s) {
        List<SetRow> sets = s.getSets().stream()
                .map(set -> new SetRow(
                        set.getId(),
                        set.getExercise() == null ? null : set.getExercise().getId(),
                        set.getSetNumber(),
                        set.getRepsDone(),
                        set.getWeightKg(),
                        set.getRpe(),
                        set.isCompleted(),
                        set.getNotes(),
                        set.isPr()))
                .toList();
        return new SessionSection(
                s.getId(),
                s.getWorkoutDayId(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getNotes(),
                s.getMood(),
                s.getEnergyLevel(),
                sets);
    }

    private static MetricRow toMetricRow(BodyMetric m) {
        return new MetricRow(
                m.getId(),
                m.getRecordedDate(),
                m.getWeightKg(),
                m.getBodyFatPercent(),
                m.getWaistCm(),
                m.getChestCm(),
                m.getArmCm(),
                m.getThighCm(),
                m.getPhotoUrl(),
                m.getNotes());
    }

    private static SupplementRow toSupplementRow(Supplement s) {
        return new SupplementRow(
                s.getId(),
                s.getName(),
                s.getDosage(),
                s.getTiming() == null ? null : s.getTiming().name().toLowerCase(),
                s.isActive());
    }
}
