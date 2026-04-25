package com.workouthub.nutrition.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FoodItemDto(
        UUID id,
        String nameTr,
        String nameEn,
        BigDecimal kcalPer100g,
        BigDecimal proteinG,
        BigDecimal carbsG,
        BigDecimal fatG,
        BigDecimal defaultServingG) {}
