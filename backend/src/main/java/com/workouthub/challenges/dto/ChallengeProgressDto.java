package com.workouthub.challenges.dto;

public record ChallengeProgressDto(
        MonthlyChallengeDto challenge,
        int currentValue,
        boolean completed) {}
