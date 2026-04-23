package com.workouthub.workouts.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkoutPlanDto(
        UUID id,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<WorkoutDayDto> days) {}
