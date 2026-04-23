package com.workouthub.users.dto;

import java.util.UUID;

public record UserMeResponse(
        UUID id,
        String email,
        String displayName,
        String role,
        UserProfileDto profile) {}
