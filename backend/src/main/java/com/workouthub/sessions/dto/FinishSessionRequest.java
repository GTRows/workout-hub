package com.workouthub.sessions.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record FinishSessionRequest(
        @Size(max = 2000) String notes,
        @Min(1) @Max(5) Short mood,
        @Min(1) @Max(5) Short energyLevel) {}
