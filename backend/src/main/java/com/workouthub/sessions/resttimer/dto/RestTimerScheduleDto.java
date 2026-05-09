package com.workouthub.sessions.resttimer.dto;

import com.workouthub.sessions.resttimer.RestTimerSchedule;
import java.time.Instant;
import java.util.UUID;

public record RestTimerScheduleDto(
        UUID id,
        UUID sessionId,
        Instant fireAt) {

    public static RestTimerScheduleDto from(RestTimerSchedule row) {
        return new RestTimerScheduleDto(row.getId(), row.getSessionId(), row.getFireAt());
    }
}
