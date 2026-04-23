package com.workouthub.sessions.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LastPerformanceDto(
        UUID sessionId,
        Instant startedAt,
        Instant endedAt,
        List<SessionSetDto> sets) {}
