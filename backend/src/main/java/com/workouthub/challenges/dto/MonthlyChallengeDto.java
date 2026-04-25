package com.workouthub.challenges.dto;

import java.util.UUID;

public record MonthlyChallengeDto(
        UUID id,
        String yearMonth,
        String nameTr,
        String nameEn,
        String descriptionTr,
        String descriptionEn,
        String ruleType,
        int threshold) {}
