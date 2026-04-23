package com.workouthub.sessions.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionDto(
        UUID id,
        UUID workoutDayId,
        Instant startedAt,
        Instant endedAt,
        String notes,
        Short mood,
        Short energyLevel,
        boolean finished,
        List<SessionSetDto> sets) {}
