package com.workouthub.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrDto(
        UUID exerciseId,
        String exerciseNameTr,
        String exerciseNameEn,
        BigDecimal estimatedOneRmKg,
        BigDecimal weightKg,
        int repsDone,
        LocalDate achievedAt) {}
