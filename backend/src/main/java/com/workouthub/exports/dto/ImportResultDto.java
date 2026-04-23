package com.workouthub.exports.dto;

public record ImportResultDto(
        int profileUpdated,
        int metricsInserted,
        int supplementsInserted,
        int plansInserted,
        int sessionsInserted,
        String userEmail) {}
