package com.workouthub.supplements.dto;

import com.workouthub.supplements.domain.SupplementTiming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record CreateSupplementRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 60) String dosage,
        @NotNull SupplementTiming timing,
        LocalTime reminderTime) {}
