package com.workouthub.workouts.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WorkoutDayExerciseDto(
        UUID id,
        UUID exerciseId,
        String exerciseNameTr,
        String exerciseNameEn,
        int orderIndex,
        int targetSets,
        Integer targetRepsMin,
        Integer targetRepsMax,
        BigDecimal targetWeightKg,
        Integer restSeconds,
        String notes) {}
