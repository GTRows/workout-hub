package com.workouthub.sessions.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SessionSetDto(
        UUID id,
        UUID exerciseId,
        String exerciseNameTr,
        String exerciseNameEn,
        short setNumber,
        short repsDone,
        BigDecimal weightKg,
        Short rpe,
        boolean completed,
        String notes) {}
