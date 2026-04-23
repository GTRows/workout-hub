package com.workouthub.workouts.dto;

import jakarta.validation.constraints.Size;

public record UpdateWorkoutPlanRequest(
        @Size(max = 120) String name) {}
