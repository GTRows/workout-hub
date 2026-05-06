package com.workouthub.exports;

import com.workouthub.exports.dto.FullExportDto;
import com.workouthub.exports.dto.FullExportDto.DayExerciseRow;
import com.workouthub.exports.dto.FullExportDto.DayRow;
import com.workouthub.exports.dto.FullExportDto.PlanSection;
import com.workouthub.exports.dto.FullExportDto.SessionSection;
import com.workouthub.exports.dto.FullExportDto.SetRow;
import com.workouthub.exports.dto.FullExportDto.SupplementRow;
import com.workouthub.supplements.domain.SupplementTiming;
import com.workouthub.workouts.domain.WorkoutFocus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Fail-soft validation pass over a full-export payload before it hits
 * the mutating import service. Errors abort the import (HTTP 422);
 * warnings and suggestions are surfaced in the response so the caller
 * (and whatever AI drafted the payload) can learn from them without
 * losing the good parts.
 */
public final class ImportValidator {

    public record ValidationReport(
            List<String> errors,
            List<String> warnings,
            List<String> suggestions) {

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private static final int LARGE_SESSION_THRESHOLD = 1000;

    private ImportValidator() {}

    public static ValidationReport validate(FullExportDto dump) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        if (dump == null) {
            errors.add("payload is null");
            return new ValidationReport(errors, warnings, suggestions);
        }

        if (dump.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
            errors.add("unsupported schemaVersion " + dump.schemaVersion()
                    + "; expected " + SUPPORTED_SCHEMA_VERSION);
        }

        validatePlans(dump.plans(), errors, warnings);
        validateSessions(dump.sessions(), warnings, suggestions);
        validateSupplements(dump.supplements(), warnings);
        validateSessionDayIds(dump.plans(), dump.sessions(), errors);

        return new ValidationReport(errors, warnings, suggestions);
    }

    private static void validatePlans(
            List<PlanSection> plans, List<String> errors, List<String> warnings) {
        if (plans == null) return;

        int activeCount = 0;
        for (PlanSection p : plans) {
            if (p.active()) activeCount++;
            if (p.name() == null || p.name().isBlank()) {
                errors.add("plan with id=" + p.id() + " has blank name");
            }
            if (p.days() == null || p.days().isEmpty()) {
                warnings.add("plan '" + p.name() + "' has no days - it will import empty");
                continue;
            }
            validatePlanDays(p, warnings);
        }
        if (activeCount > 1) {
            errors.add("at most one plan may be active; payload has " + activeCount);
        }
    }

    private static void validatePlanDays(PlanSection p, List<String> warnings) {
        Set<Short> seenDaysOfWeek = new HashSet<>();
        for (DayRow d : p.days()) {
            if (d.dayOfWeek() < 1 || d.dayOfWeek() > 7) {
                warnings.add("plan '" + p.name() + "' day has dayOfWeek="
                        + d.dayOfWeek() + " (must be 1-7)");
            } else if (!seenDaysOfWeek.add(d.dayOfWeek())) {
                warnings.add("plan '" + p.name()
                        + "' has two days on dayOfWeek=" + d.dayOfWeek());
            }
            if (d.focus() != null && !isValidFocus(d.focus())) {
                warnings.add("plan '" + p.name() + "' day '" + d.name()
                        + "' uses unknown focus '" + d.focus()
                        + "' - will default to 'push'");
            }
            if (d.exercises() != null) {
                for (DayExerciseRow e : d.exercises()) {
                    if (e.exerciseId() == null) {
                        warnings.add("plan '" + p.name() + "' day '" + d.name()
                                + "' has a row with no exerciseId - that row will be skipped");
                    }
                    if (e.targetSets() <= 0) {
                        warnings.add("plan '" + p.name() + "' day '" + d.name()
                                + "' has targetSets=" + e.targetSets()
                                + " (expected a positive integer)");
                    }
                }
            }
        }
    }

    private static void validateSessions(
            List<SessionSection> sessions,
            List<String> warnings,
            List<String> suggestions) {
        if (sessions == null) return;

        if (sessions.size() > LARGE_SESSION_THRESHOLD) {
            suggestions.add("payload has " + sessions.size()
                    + " sessions; some LLMs will truncate. Consider splitting by year.");
        }

        for (SessionSection s : sessions) {
            if (s.sets() == null || s.sets().isEmpty()) {
                warnings.add("session " + s.startedAt()
                        + " has no sets - it will import as an empty session");
                continue;
            }
            for (SetRow r : s.sets()) {
                if (r.exerciseId() == null) {
                    warnings.add("session " + s.startedAt()
                            + " set #" + r.setNumber()
                            + " has no exerciseId - that set will be skipped");
                }
                if (r.repsDone() < 0) {
                    warnings.add("session " + s.startedAt()
                            + " set #" + r.setNumber()
                            + " has negative repsDone=" + r.repsDone());
                }
            }
        }
    }

    private static void validateSupplements(
            List<SupplementRow> rows, List<String> warnings) {
        if (rows == null) return;
        for (SupplementRow s : rows) {
            if (s.timing() != null && !isValidTiming(s.timing())) {
                warnings.add("supplement '" + s.name() + "' has unknown timing '"
                        + s.timing() + "' - will default to 'other'");
            }
        }
    }

    private static void validateSessionDayIds(
            List<PlanSection> plans,
            List<SessionSection> sessions,
            List<String> errors) {
        if (sessions == null || sessions.isEmpty()) return;
        Set<UUID> declaredDayIds = new HashSet<>();
        if (plans != null) {
            for (PlanSection p : plans) {
                if (p.days() == null) continue;
                for (DayRow d : p.days()) {
                    if (d.id() != null) declaredDayIds.add(d.id());
                }
            }
        }
        for (SessionSection s : sessions) {
            if (s.workoutDayId() == null) continue;
            if (!declaredDayIds.contains(s.workoutDayId())) {
                errors.add("session " + s.id()
                        + " references workoutDayId=" + s.workoutDayId()
                        + " which is not present in plans[].days[].id");
            }
        }
    }

    private static boolean isValidFocus(String value) {
        try {
            WorkoutFocus.valueOf(value.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static boolean isValidTiming(String value) {
        try {
            SupplementTiming.valueOf(value.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
