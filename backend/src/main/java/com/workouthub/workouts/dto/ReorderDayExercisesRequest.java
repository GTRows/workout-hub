package com.workouthub.workouts.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderDayExercisesRequest(
        @NotEmpty List<UUID> itemIdsInOrder) {}
