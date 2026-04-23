package com.workouthub.workouts.dto;

import java.util.List;
import java.util.UUID;

public record WorkoutDayDto(
        UUID id,
        short dayOfWeek,
        String name,
        String focus,
        Integer estimatedDurationMin,
        List<WorkoutDayExerciseDto> exercises) {}
