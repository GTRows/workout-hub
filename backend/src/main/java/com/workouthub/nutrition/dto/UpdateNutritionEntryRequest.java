package com.workouthub.nutrition.dto;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateNutritionEntryRequest(
        UUID foodId,
        @DecimalMin("0.1") BigDecimal servingG,
        Instant consumedAt,
        String notes) {}
