package com.workouthub.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OneRmPointDto(
        LocalDate date,
        BigDecimal estimatedOneRmKg,
        int repsDone,
        BigDecimal weightKg) {}
