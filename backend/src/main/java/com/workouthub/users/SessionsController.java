package com.workouthub.users;

import com.workouthub.auth.domain.RefreshToken;
import com.workouthub.auth.domain.RefreshTokenRepository;
import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.common.web.NotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("userSessionsController")
@RequestMapping("/api/users/me/sessions")
@Tag(name = "User Sessions (refresh tokens)", description = "Active refresh-token sessions for the current user with revocation.")
public class SessionsController {

    public record SessionDto(
            UUID id,
            String userAgent,
            Instant createdAt,
            Instant lastUsedAt,
            Instant expiresAt) {}

    private final RefreshTokenRepository refreshTokens;

    public SessionsController(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @GetMapping
    public List<SessionDto> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return refreshTokens
                .findByUserIdAndRevokedFalseOrderByCreatedAtDesc(principal.userId())
                .stream()
                .map(rt -> new SessionDto(
                        rt.getId(),
                        rt.getUserAgent(),
                        rt.getCreatedAt(),
                        rt.getLastUsedAt(),
                        rt.getExpiresAt()))
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        RefreshToken token = refreshTokens.findByIdAndUserId(id, principal.userId())
                .orElseThrow(() -> new NotFoundException("Session not found: " + id));
        token.setRevoked(true);
        refreshTokens.save(token);
        return ResponseEntity.noContent().build();
    }
}
