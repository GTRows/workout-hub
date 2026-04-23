package com.workouthub.users.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UserProfileDto(
        Integer heightCm,
        BigDecimal weightKg,
        LocalDate birthDate,
        String gender,
        String healthNotes,
        String goals) {}
