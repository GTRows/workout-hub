package com.workouthub.supplements.dto;

import com.workouthub.supplements.domain.SupplementTiming;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record UpdateSupplementRequest(
        @Size(max = 120) String name,
        @Size(max = 60) String dosage,
        SupplementTiming timing,
        Boolean active,
        LocalTime reminderTime) {}
