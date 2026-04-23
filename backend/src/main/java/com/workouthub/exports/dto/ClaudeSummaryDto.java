package com.workouthub.exports.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Compact last-N-days summary shaped for pasting into Claude. Mirrors the
 * schema in ProjectBrief's "AI Koç" section, with body_metrics and
 * consistency.missed_reasons left empty until PHASE 5's metrics surface
 * lands.
 */
public record ClaudeSummaryDto(
        UserSummary user,
        Period period,
        Totals summary,
        List<WorkoutEntry> workouts) {

    public record UserSummary(
            String displayName,
            String email,
            Integer heightCm,
            BigDecimal weightKg,
            String healthNotes,
            String goals) {}

    public record Period(LocalDate from, LocalDate to, int days) {}

    public record Totals(
            int totalWorkouts,
            BigDecimal totalVolumeKg,
            Long avgSessionDurationMin) {}

    public record WorkoutEntry(
            LocalDate date,
            Long durationMin,
            List<ExerciseEntry> exercises,
            String userNotes,
            Short mood,
            Short energyLevel) {}

    public record ExerciseEntry(
            String nameTr,
            String nameEn,
            List<SetEntry> sets) {}

    public record SetEntry(short repsDone, BigDecimal weightKg) {}
}
