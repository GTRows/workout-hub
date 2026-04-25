package com.workouthub.challenges.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpsertChallengeRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{4}-\\d{2}$",
                message = "yearMonth must be 'yyyy-MM'")
        String yearMonth,

        @NotBlank String nameTr,
        @NotBlank String nameEn,
        String descriptionTr,
        String descriptionEn,
        @NotBlank String ruleType,
        @Min(1) int threshold) {}
