package com.workouthub.common.security.test;

import com.workouthub.common.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller (lives under src/test/java) that exposes the current
 * authenticated principal. Used by JwtAuthenticationFilterTest to verify
 * that the filter populates the security context for valid access tokens.
 */
@RestController
public class TestSecuredController {

    @GetMapping("/api/test-secured/me")
    public String me(@AuthenticationPrincipal AppUserPrincipal principal) {
        return principal == null ? "anonymous" : principal.userId().toString();
    }
}
