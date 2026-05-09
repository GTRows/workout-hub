package com.workouthub.sessions.resttimer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleRestTimerRequest(
        @NotNull @Min(1) @Max(3600) Integer seconds,
        @NotBlank @Size(max = 500) String title,
        @NotBlank @Size(max = 500) String body,
        @Size(max = 500) String clickUrl) {}
