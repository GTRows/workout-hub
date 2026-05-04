package com.workouthub.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProgressPointDto(
        UUID sessionId,
        Instant startedAt,
        int setCount,
        BigDecimal totalVolumeKg,
        BigDecimal maxWeightKg,
        short topRepsDone) {}
