package com.workouthub.metrics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BodyMetricDto(
        UUID id,
        LocalDate recordedDate,
        BigDecimal weightKg,
        BigDecimal bodyFatPercent,
        BigDecimal waistCm,
        BigDecimal chestCm,
        BigDecimal armCm,
        BigDecimal thighCm,
        String photoUrl,
        String notes,
        Instant createdAt,
        Instant updatedAt) {}
