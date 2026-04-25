package com.workouthub.health.dto;

public record HealthImportResultDto(
        int bodyMassParsed,
        int bodyMassImported,
        int bodyMassSkipped,
        int workoutsParsed,
        int workoutsImported,
        int workoutsSkipped) {}
