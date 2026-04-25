package com.workouthub.nutrition.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NutritionEntryDto(
        UUID id,
        UUID foodId,
        String foodNameTr,
        String foodNameEn,
        BigDecimal servingG,
        BigDecimal kcal,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        Instant consumedAt,
        String notes) {}
