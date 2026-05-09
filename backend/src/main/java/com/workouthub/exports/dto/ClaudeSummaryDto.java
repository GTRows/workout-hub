package com.workouthub.exports.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Compact last-N-days summary shaped for pasting into Claude. Mirrors the
 * schema in ProjectBrief's "AI Koc" section: profile snapshot, period window,
 * aggregate totals, per-session detail, in-window personal records, weight
 * trend, and consistency metrics.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ClaudeSummaryDto(
        UserSummary user,
        Period period,
        Totals summary,
        List<WorkoutEntry> workouts,
        List<PrEntry> prs,
        List<BodyMetricSummary> bodyMetrics,
        Consistency consistency) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UserSummary(
            String displayName,
            Integer age,
            Integer heightCm,
            BigDecimal weightKg,
            String healthNotes,
            List<String> goals) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Period(LocalDate from, LocalDate to, int days) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Totals(
            int totalWorkouts,
            Integer plannedWorkouts,
            Integer adherencePercent,
            BigDecimal totalVolumeKg,
            Long avgSessionDurationMin,
            BigDecimal weightChangeKg) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record WorkoutEntry(
            LocalDate date,
            String type,
            Long durationMin,
            List<ExerciseEntry> exercises,
            String userNotes,
            Short mood,
            Short energy) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ExerciseEntry(
            String nameTr,
            String nameEn,
            List<SetEntry> sets) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record SetEntry(BigDecimal weight, short reps) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record PrEntry(
            String exercise,
            BigDecimal weight,
            short reps,
            LocalDate date) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record BodyMetricSummary(
            LocalDate date,
            BigDecimal weightKg) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Consistency(
            int currentStreakDays,
            List<LocalDate> missedDays,
            List<String> missedReasons) {}
}
