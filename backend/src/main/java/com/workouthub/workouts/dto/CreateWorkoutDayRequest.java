package com.workouthub.workouts.dto;

import com.workouthub.workouts.domain.WorkoutFocus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkoutDayRequest(
        @NotNull @Min(1) @Max(7) Short dayOfWeek,
        @NotBlank @Size(max = 120) String name,
        @NotNull WorkoutFocus focus,
        @Min(5) @Max(600) Integer estimatedDurationMin) {}
