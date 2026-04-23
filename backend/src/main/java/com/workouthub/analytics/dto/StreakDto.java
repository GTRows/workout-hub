package com.workouthub.analytics.dto;

import java.time.LocalDate;

public record StreakDto(
        int currentStreakDays,
        int longestStreakDays,
        LocalDate lastSessionDate) {}
