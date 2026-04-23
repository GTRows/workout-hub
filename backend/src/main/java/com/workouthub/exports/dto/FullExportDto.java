package com.workouthub.exports.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FullExportDto(
        int schemaVersion,
        Instant exportedAt,
        UserSection user,
        List<PlanSection> plans,
        List<SessionSection> sessions,
        List<MetricRow> bodyMetrics,
        List<SupplementRow> supplements) {

    public record UserSection(
            UUID id,
            String email,
            String displayName,
            Integer heightCm,
            BigDecimal weightKg,
            LocalDate birthDate,
            String gender,
            String healthNotes,
            String goals) {}

    public record PlanSection(
            UUID id,
            String name,
            boolean active,
            List<DayRow> days) {}

    public record DayRow(
            UUID id,
            short dayOfWeek,
            String name,
            String focus,
            Integer estimatedDurationMin,
            List<DayExerciseRow> exercises) {}

    public record DayExerciseRow(
            UUID id,
            UUID exerciseId,
            int orderIndex,
            int targetSets,
            Integer targetRepsMin,
            Integer targetRepsMax,
            BigDecimal targetWeightKg,
            Integer restSeconds,
            String notes) {}

    public record SessionSection(
            UUID id,
            UUID workoutDayId,
            Instant startedAt,
            Instant endedAt,
            String notes,
            Short mood,
            Short energyLevel,
            List<SetRow> sets) {}

    public record SetRow(
            UUID id,
            UUID exerciseId,
            short setNumber,
            short repsDone,
            BigDecimal weightKg,
            Short rpe,
            boolean completed,
            String notes) {}

    public record MetricRow(
            UUID id,
            LocalDate recordedDate,
            BigDecimal weightKg,
            BigDecimal bodyFatPercent,
            BigDecimal waistCm,
            BigDecimal chestCm,
            BigDecimal armCm,
            BigDecimal thighCm,
            String photoUrl,
            String notes) {}

    public record SupplementRow(
            UUID id,
            String name,
            String dosage,
            String timing,
            boolean active) {}
}
