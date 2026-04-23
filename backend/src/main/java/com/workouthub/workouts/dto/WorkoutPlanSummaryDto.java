package com.workouthub.workouts.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkoutPlanSummaryDto(
        UUID id,
        String name,
        boolean active,
        int dayCount,
        Instant createdAt,
        Instant updatedAt) {}
