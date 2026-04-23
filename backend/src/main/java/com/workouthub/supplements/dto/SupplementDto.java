package com.workouthub.supplements.dto;

import com.workouthub.supplements.domain.SupplementTiming;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record SupplementDto(
        UUID id,
        String name,
        String dosage,
        SupplementTiming timing,
        boolean active,
        LocalTime reminderTime,
        Instant createdAt,
        Instant updatedAt) {}
