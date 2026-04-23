package com.workouthub.metrics.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpsertBodyMetricRequest(
        @NotNull LocalDate recordedDate,
        @DecimalMin("20.0") @DecimalMax("500.0") BigDecimal weightKg,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal bodyFatPercent,
        @DecimalMin("20.0") @DecimalMax("300.0") BigDecimal waistCm,
        @DecimalMin("20.0") @DecimalMax("300.0") BigDecimal chestCm,
        @DecimalMin("10.0") @DecimalMax("100.0") BigDecimal armCm,
        @DecimalMin("20.0") @DecimalMax("200.0") BigDecimal thighCm,
        @Size(max = 2000) String notes) {}
