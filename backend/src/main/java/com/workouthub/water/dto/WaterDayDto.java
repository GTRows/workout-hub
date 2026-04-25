package com.workouthub.water.dto;

import java.time.LocalDate;
import java.util.List;

public record WaterDayDto(
        LocalDate date,
        int totalMl,
        List<WaterEntryDto> entries) {}
