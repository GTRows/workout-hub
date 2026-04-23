package com.workouthub.workouts;

import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutDayExercise;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.dto.WorkoutDayDto;
import com.workouthub.workouts.dto.WorkoutDayExerciseDto;
import com.workouthub.workouts.dto.WorkoutPlanDto;
import com.workouthub.workouts.dto.WorkoutPlanSummaryDto;

public final class WorkoutPlanMapper {

    private WorkoutPlanMapper() {}

    public static WorkoutPlanSummaryDto toSummary(WorkoutPlan plan) {
        return new WorkoutPlanSummaryDto(
                plan.getId(),
                plan.getName(),
                plan.isActive(),
                plan.getDays() == null ? 0 : plan.getDays().size(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    public static WorkoutPlanDto toDto(WorkoutPlan plan) {
        return new WorkoutPlanDto(
                plan.getId(),
                plan.getName(),
                plan.isActive(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                plan.getDays().stream().map(WorkoutPlanMapper::toDayDto).toList());
    }

    public static WorkoutDayDto toDayDto(WorkoutDay day) {
        return new WorkoutDayDto(
                day.getId(),
                day.getDayOfWeek(),
                day.getName(),
                day.getFocus() == null ? null : day.getFocus().name().toLowerCase(),
                day.getEstimatedDurationMin(),
                day.getExercises().stream().map(WorkoutPlanMapper::toItemDto).toList());
    }

    public static WorkoutDayExerciseDto toItemDto(WorkoutDayExercise item) {
        var exercise = item.getExercise();
        return new WorkoutDayExerciseDto(
                item.getId(),
                exercise == null ? null : exercise.getId(),
                exercise == null ? null : exercise.getNameTr(),
                exercise == null ? null : exercise.getNameEn(),
                item.getOrderIndex(),
                item.getTargetSets(),
                item.getTargetRepsMin(),
                item.getTargetRepsMax(),
                item.getTargetWeightKg(),
                item.getRestSeconds(),
                item.getNotes());
    }
}
