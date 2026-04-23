package com.workouthub.workouts.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AddDayExerciseRequest(
        @NotNull UUID exerciseId,
        @NotNull @Min(1) Integer targetSets,
        @Min(1) Integer targetRepsMin,
        @Min(1) Integer targetRepsMax,
        @DecimalMin("0.0") @DecimalMax("999.99") BigDecimal targetWeightKg,
        @Min(0) Integer restSeconds,
        @Size(max = 1000) String notes) {}
