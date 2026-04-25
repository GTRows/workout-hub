package com.workouthub.health;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ParsedHealth(
        List<BodyMassRecord> bodyMass,
        List<WorkoutRecord> workouts) {

    public record BodyMassRecord(LocalDate date, BigDecimal kg) {}

    public record WorkoutRecord(
            String activityType,
            Instant startedAt,
            Instant endedAt,
            String notes) {}
}
