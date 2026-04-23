package com.workouthub.workouts.dto;

import com.workouthub.workouts.domain.WorkoutFocus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateWorkoutDayRequest(
        @Min(1) @Max(7) Short dayOfWeek,
        @Size(max = 120) String name,
        WorkoutFocus focus,
        @Min(5) @Max(600) Integer estimatedDurationMin) {}
