package com.workouthub.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        Instant createdAt,
        Instant updatedAt) {}
