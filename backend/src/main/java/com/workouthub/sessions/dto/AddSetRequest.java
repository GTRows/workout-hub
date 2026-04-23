package com.workouthub.sessions.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record AddSetRequest(
        @NotNull UUID exerciseId,
        @Min(1) Short setNumber,
        @NotNull @Min(0) Short repsDone,
        @DecimalMin("0.0") @DecimalMax("999.99") BigDecimal weightKg,
        @Min(1) @Max(10) Short rpe,
        Boolean completed,
        @Size(max = 1000) String notes) {}
