package com.workouthub.users.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 100) String displayName,
        @Min(50) @Max(300) Integer heightCm,
        @DecimalMin("20.0") @DecimalMax("500.0") BigDecimal weightKg,
        LocalDate birthDate,
        @Size(max = 20) String gender,
        @Size(max = 10000) String healthNotes,
        @Size(max = 10000) String goals,
        @Min(0) @Max(20000) Integer dailyKcalGoal,
        @Min(0) @Max(1000) Integer dailyProteinGGoal,
        @Min(0) @Max(2000) Integer dailyCarbsGGoal,
        @Min(0) @Max(1000) Integer dailyFatGGoal) {}
