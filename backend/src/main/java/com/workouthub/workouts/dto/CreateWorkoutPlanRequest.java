package com.workouthub.workouts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkoutPlanRequest(
        @NotBlank @Size(max = 120) String name) {}
