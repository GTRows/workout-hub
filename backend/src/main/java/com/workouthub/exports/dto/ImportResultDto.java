package com.workouthub.exports.dto;

import java.util.List;

public record ImportResultDto(
        int profileUpdated,
        int metricsInserted,
        int supplementsInserted,
        int plansInserted,
        int sessionsInserted,
        String userEmail,
        List<String> warnings,
        List<String> suggestions) {

    public ImportResultDto(
            int profileUpdated,
            int metricsInserted,
            int supplementsInserted,
            int plansInserted,
            int sessionsInserted,
            String userEmail) {
        this(profileUpdated, metricsInserted, supplementsInserted,
                plansInserted, sessionsInserted, userEmail, List.of(), List.of());
    }
}
