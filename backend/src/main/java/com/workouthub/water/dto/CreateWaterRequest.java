package com.workouthub.water.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;

public record CreateWaterRequest(
        @Min(1) @Max(5000) int ml,
        Instant consumedAt) {}
