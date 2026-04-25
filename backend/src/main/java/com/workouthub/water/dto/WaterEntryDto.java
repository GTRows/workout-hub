package com.workouthub.water.dto;

import java.time.Instant;
import java.util.UUID;

public record WaterEntryDto(UUID id, int ml, Instant consumedAt) {}
