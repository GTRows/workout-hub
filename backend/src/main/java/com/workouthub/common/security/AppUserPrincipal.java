package com.workouthub.common.security;

import java.util.UUID;

public record AppUserPrincipal(UUID userId, String role) {}
