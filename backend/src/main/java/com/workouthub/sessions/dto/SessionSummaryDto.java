package com.workouthub.sessions.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionSummaryDto(
        UUID id,
        UUID workoutDayId,
        Instant startedAt,
        Instant endedAt,
        boolean finished,
        int setCount,
        Short mood,
        Short energyLevel) {}
