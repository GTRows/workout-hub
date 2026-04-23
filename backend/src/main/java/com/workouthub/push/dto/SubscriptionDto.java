package com.workouthub.push.dto;

import java.time.Instant;
import java.util.UUID;

public record SubscriptionDto(
        UUID id,
        String endpoint,
        Instant createdAt,
        Instant updatedAt) {}
