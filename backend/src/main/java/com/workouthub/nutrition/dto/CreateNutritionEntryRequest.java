package com.workouthub.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateNutritionEntryRequest(
        @NotNull UUID foodId,
        @NotNull @DecimalMin("0.1") BigDecimal servingG,
        @NotNull Instant consumedAt,
        String notes) {}
