package com.workouthub.exports;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exports.dto.FullExportDto;
import com.workouthub.exports.dto.ImportResultDto;
import com.workouthub.metrics.domain.BodyMetric;
import com.workouthub.metrics.domain.BodyMetricRepository;
import com.workouthub.sessions.SessionSetsService;
import com.workouthub.sessions.domain.SessionSet;
import com.workouthub.sessions.domain.WorkoutSession;
import com.workouthub.sessions.domain.WorkoutSessionRepository;
import com.workouthub.supplements.domain.Supplement;
import com.workouthub.supplements.domain.SupplementRepository;
import com.workouthub.supplements.domain.SupplementTiming;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutDayExercise;
import com.workouthub.workouts.domain.WorkoutFocus;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Idempotent import of the full-export dump. Delete-then-reinsert replace
 * semantics per slice, under a single transaction so a FK violation
 * rolls back the entire import.
 */
@Service
@Transactional
public class FullImportService {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final BodyMetricRepository metrics;
    private final SupplementRepository supplements;
    private final WorkoutPlanRepository plans;
    private final WorkoutSessionRepository sessions;
    private final SessionSetsService sessionSets;

    @PersistenceContext
    private EntityManager em;

    public FullImportService(
            UserRepository users,
            UserProfileRepository profiles,
            BodyMetricRepository metrics,
            SupplementRepository supplements,
            WorkoutPlanRepository plans,
            WorkoutSessionRepository sessions,
            SessionSetsService sessionSets) {
        this.users = users;
        this.profiles = profiles;
        this.metrics = metrics;
        this.supplements = supplements;
        this.plans = plans;
        this.sessions = sessions;
        this.sessionSets = sessionSets;
    }

    public ImportResultDto importDump(UUID userId, FullExportDto dump) {
        if (dump == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty import payload");
        }
        ImportValidator.ValidationReport report = ImportValidator.validate(dump);
        if (report.hasErrors()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Validation failed: " + String.join("; ", report.errors()));
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        int profileUpdated = doImportProfile(userId, dump.user());
        int metricsInserted = replaceMetrics(userId, dump.bodyMetrics());
        int supplementsInserted = replaceSupplements(userId, dump.supplements());

        // Sessions reference workout_days via a nullable FK. Delete sessions
        // first so plans can be cascade-dropped without violating FKs, then
        // re-insert plans before sessions.
        int sessionsDeleted = wipeSessions(userId);
        Map<UUID, UUID> dayIdRewrite = new HashMap<>();
        int plansInserted = replacePlans(userId, dump.plans(), dayIdRewrite);
        Set<UUID> touchedExerciseIds = new LinkedHashSet<>();
        int sessionsInserted = insertSessions(
                userId, dump.sessions(), touchedExerciseIds, dayIdRewrite);
        for (UUID exerciseId : touchedExerciseIds) {
            sessionSets.recomputePrForExerciseHistory(userId, exerciseId);
        }

        // sessionsDeleted is currently unused but kept so the logic-line is
        // explicit; suppress the unused-variable warning.
        if (sessionsDeleted < 0) throw new IllegalStateException();

        return new ImportResultDto(
                profileUpdated,
                metricsInserted,
                supplementsInserted,
                plansInserted,
                sessionsInserted,
                user.getEmail(),
                report.warnings(),
                report.suggestions());
    }

    public int importProfile(UUID userId, FullExportDto.UserSection section) {
        return doImportProfile(userId, section);
    }

    public int replaceMetricsSection(UUID userId, List<FullExportDto.MetricRow> rows) {
        return replaceMetrics(userId, rows);
    }

    public int replaceSupplementsSection(UUID userId, List<FullExportDto.SupplementRow> rows) {
        return replaceSupplements(userId, rows);
    }

    public int replacePlansSection(UUID userId, List<FullExportDto.PlanSection> rows) {
        // Plans are FK'd from workout_sessions.workout_day_id (nullable).
        // Null out those references so cascade-drop of plans -> days cannot
        // violate the FK, then rebuild plans. Sessions keep their history
        // but become "ad-hoc" until the user reattaches them to a new day.
        sessions.detachSessionsFromDays(userId);
        return replacePlans(userId, rows, new HashMap<>());
    }

    public int replaceSessionsSection(UUID userId, List<FullExportDto.SessionSection> rows) {
        wipeSessions(userId);
        Set<UUID> touchedExerciseIds = new LinkedHashSet<>();
        int inserted = insertSessions(userId, rows, touchedExerciseIds, Map.of());
        for (UUID exerciseId : touchedExerciseIds) {
            sessionSets.recomputePrForExerciseHistory(userId, exerciseId);
        }
        return inserted;
    }

    private int doImportProfile(UUID userId, FullExportDto.UserSection section) {
        if (section == null) return 0;
        UserProfile profile = profiles.findById(userId).orElseGet(() -> {
            UserProfile fresh = new UserProfile();
            User owner = users.findById(userId).orElseThrow();
            fresh.setUser(owner);
            return fresh;
        });
        profile.setHeightCm(section.heightCm());
        profile.setWeightKg(section.weightKg());
        profile.setBirthDate(section.birthDate());
        profile.setGender(section.gender());
        profile.setHealthNotes(section.healthNotes());
        profile.setGoals(section.goals());
        profiles.save(profile);
        return 1;
    }

    private int replaceMetrics(UUID userId, List<FullExportDto.MetricRow> rows) {
        if (rows == null) return 0;
        List<BodyMetric> existing = metrics.findByUserIdOrderByRecordedDateDesc(userId);
        metrics.deleteAll(existing);
        metrics.flush();

        List<BodyMetric> fresh = new ArrayList<>(rows.size());
        for (FullExportDto.MetricRow r : rows) {
            BodyMetric m = new BodyMetric();
            m.setUserId(userId);
            m.setRecordedDate(r.recordedDate());
            m.setWeightKg(r.weightKg());
            m.setBodyFatPercent(r.bodyFatPercent());
            m.setWaistCm(r.waistCm());
            m.setChestCm(r.chestCm());
            m.setArmCm(r.armCm());
            m.setThighCm(r.thighCm());
            m.setPhotoUrl(r.photoUrl());
            m.setNotes(r.notes());
            fresh.add(m);
        }
        metrics.saveAll(fresh);
        return fresh.size();
    }

    private int replaceSupplements(UUID userId, List<FullExportDto.SupplementRow> rows) {
        if (rows == null) return 0;
        List<Supplement> existing = supplements.findByUserIdOrderByCreatedAtAsc(userId);
        supplements.deleteAll(existing);
        supplements.flush();

        List<Supplement> fresh = new ArrayList<>(rows.size());
        for (FullExportDto.SupplementRow r : rows) {
            Supplement s = new Supplement();
            s.setUserId(userId);
            s.setName(r.name());
            s.setDosage(r.dosage());
            s.setTiming(parseTiming(r.timing()));
            s.setActive(r.active());
            fresh.add(s);
        }
        supplements.saveAll(fresh);
        return fresh.size();
    }

    private int wipeSessions(UUID userId) {
        List<WorkoutSession> finished =
                sessions.findByUserIdAndEndedAtIsNotNullOrderByStartedAtDesc(userId);
        int deleted = finished.size();
        sessions.deleteAll(finished);
        sessions.findByUserIdAndEndedAtIsNull(userId).ifPresent(sub -> {
            sessions.delete(sub);
        });
        sessions.flush();
        return deleted;
    }

    private int replacePlans(
            UUID userId,
            List<FullExportDto.PlanSection> rows,
            Map<UUID, UUID> dayIdRewriteOut) {
        List<WorkoutPlan> existing = plans.findByUserIdOrderByCreatedAtAsc(userId);
        plans.deleteAll(existing);
        plans.flush();

        if (rows == null || rows.isEmpty()) return 0;

        int activeCount = 0;
        for (FullExportDto.PlanSection section : rows) {
            if (section.active()) activeCount++;
        }
        if (activeCount > 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Exactly one active plan is allowed; payload has " + activeCount);
        }

        int count = 0;
        for (FullExportDto.PlanSection section : rows) {
            // Do NOT preserve the export's plan/day/exercise UUIDs. With
            // @UuidGenerator and no @Version, Hibernate cannot reliably
            // distinguish a brand-new entity-with-assigned-id from a detached
            // one in the same persistence context that just deleted that row,
            // and routes the save through merge -> UPDATE -> StaleObjectState.
            // Sessions reference plan-day ids; we maintain dayIdRewriteOut so
            // the session insert pass can rewrite session.workoutDayId from
            // payload UUIDs to the freshly-generated DB UUIDs.
            WorkoutPlan plan = new WorkoutPlan();
            plan.setUserId(userId);
            plan.setName(section.name());
            plan.setActive(section.active());

            if (section.days() != null) {
                for (FullExportDto.DayRow dr : section.days()) {
                    WorkoutDay day = new WorkoutDay();
                    day.setDayOfWeek(dr.dayOfWeek());
                    day.setName(dr.name());
                    day.setFocus(parseFocus(dr.focus()));
                    day.setEstimatedDurationMin(dr.estimatedDurationMin());

                    if (dr.exercises() != null) {
                        for (FullExportDto.DayExerciseRow er : dr.exercises()) {
                            if (er.exerciseId() == null) continue;
                            WorkoutDayExercise item = new WorkoutDayExercise();
                            item.setExercise(em.getReference(Exercise.class, er.exerciseId()));
                            item.setOrderIndex(er.orderIndex());
                            item.setTargetSets(er.targetSets());
                            item.setTargetRepsMin(er.targetRepsMin());
                            item.setTargetRepsMax(er.targetRepsMax());
                            item.setTargetWeightKg(er.targetWeightKg());
                            item.setRestSeconds(er.restSeconds());
                            item.setNotes(er.notes());
                            day.addExercise(item);
                        }
                    }
                    plan.addDay(day);
                    // After persist (below) day.getId() is non-null; flush the
                    // plan first so cascade fires and we can read the new ids.
                    if (dr.id() != null) {
                        dayIdRewriteOut.put(dr.id(), null);
                        // sentinel: real value patched after flush below
                    }
                }
            }
            plans.save(plan);
            // Flush so cascade-generated UUIDs on the WorkoutDay rows are
            // available; then resolve the payload-id -> DB-id mapping by
            // walking the just-persisted day list in declaration order.
            plans.flush();
            if (section.days() != null) {
                List<WorkoutDay> persistedDays = plan.getDays();
                List<FullExportDto.DayRow> payloadDays = section.days();
                int pairs = Math.min(persistedDays.size(), payloadDays.size());
                for (int i = 0; i < pairs; i++) {
                    UUID payloadId = payloadDays.get(i).id();
                    if (payloadId != null) {
                        dayIdRewriteOut.put(payloadId, persistedDays.get(i).getId());
                    }
                }
            }
            count++;
        }
        return count;
    }

    private int insertSessions(
            UUID userId,
            List<FullExportDto.SessionSection> rows,
            Set<UUID> touchedExerciseIds,
            Map<UUID, UUID> dayIdRewrite) {
        if (rows == null || rows.isEmpty()) return 0;

        int count = 0;
        for (FullExportDto.SessionSection section : rows) {
            // Same rationale as replacePlans: do not preserve export UUIDs.
            // Rewrite session.workoutDayId from the payload's plan-day id to
            // the freshly-generated DB id so the FK still points at a real row.
            WorkoutSession s = new WorkoutSession();
            s.setUserId(userId);
            UUID payloadDayId = section.workoutDayId();
            UUID rewrittenDayId = payloadDayId == null
                    ? null
                    : dayIdRewrite.getOrDefault(payloadDayId, payloadDayId);
            s.setWorkoutDayId(rewrittenDayId);
            s.setStartedAt(section.startedAt());
            s.setEndedAt(section.endedAt());
            s.setNotes(section.notes());
            s.setMood(section.mood());
            s.setEnergyLevel(section.energyLevel());

            if (section.sets() != null) {
                for (FullExportDto.SetRow r : section.sets()) {
                    if (r.exerciseId() == null) continue;
                    touchedExerciseIds.add(r.exerciseId());
                    SessionSet set = new SessionSet();
                    set.setExercise(em.getReference(Exercise.class, r.exerciseId()));
                    set.setSetNumber(r.setNumber());
                    set.setRepsDone(r.repsDone());
                    set.setWeightKg(r.weightKg());
                    set.setRpe(r.rpe());
                    set.setCompleted(r.completed());
                    set.setPr(r.isPr() != null && r.isPr());
                    set.setNotes(r.notes());
                    s.addSet(set);
                }
            }
            sessions.save(s);
            count++;
        }
        return count;
    }

    private static WorkoutFocus parseFocus(String s) {
        if (s == null) return WorkoutFocus.PUSH;
        try {
            return WorkoutFocus.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unknown workout focus value: " + s);
        }
    }

    private static SupplementTiming parseTiming(String s) {
        if (s == null) return SupplementTiming.OTHER;
        try {
            return SupplementTiming.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unknown supplement timing value: " + s);
        }
    }
}
