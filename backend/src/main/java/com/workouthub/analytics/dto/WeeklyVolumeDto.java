package com.workouthub.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyVolumeDto(
        LocalDate weekStart,
        BigDecimal totalVolumeKg,
        int sessionCount) {}
