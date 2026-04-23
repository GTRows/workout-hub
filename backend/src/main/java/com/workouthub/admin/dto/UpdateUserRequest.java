package com.workouthub.admin.dto;

import com.workouthub.users.domain.Role;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 100) String displayName,
        Role role) {}
