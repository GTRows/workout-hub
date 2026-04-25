package com.workouthub.webhooks.dto;

import java.time.Instant;
import java.util.UUID;

public record WebhookTokenDto(
        UUID id,
        String token,
        String purpose,
        Instant createdAt,
        Instant lastUsedAt) {}
