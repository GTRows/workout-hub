package com.workouthub.analytics.dto;

import java.time.LocalDate;

public record HeatmapDayDto(LocalDate date, int sessionCount) {}
