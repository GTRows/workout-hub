package com.workouthub.achievements.dto;

import java.time.Instant;
import java.util.UUID;

public record AchievementDto(
        UUID id,
        String code,
        String nameTr,
        String nameEn,
        String descriptionTr,
        String descriptionEn,
        String icon,
        String ruleType,
        int threshold,
        boolean unlocked,
        Instant unlockedAt,
        Integer progressValue) {}
