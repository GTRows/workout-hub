package com.workouthub.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET_A =
            "test-jwt-secret-at-least-32-bytes-long-for-hs256-0123456789";
    private static final String SECRET_B =
            "another-jwt-secret-of-at-least-32-bytes-length-abcdef-123456";

    private final JwtService service = new JwtService(SECRET_A, 3600, 1_209_600);

    @Test
    void accessTokenCarriesUserIdRoleAndAccessType() {
        UUID userId = UUID.randomUUID();

        String token = service.generateAccessToken(userId, "USER");
        var claims = service.parse(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.get("type", String.class)).isEqualTo(JwtService.TYPE_ACCESS);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void refreshTokenTypeDiffersFromAccess() {
        String token = service.generateRefreshToken(UUID.randomUUID(), "USER");

        var claims = service.parse(token);

        assertThat(claims.get("type", String.class)).isEqualTo(JwtService.TYPE_REFRESH);
    }

    @Test
    void tokenSignedWithDifferentSecretFailsVerification() {
        JwtService other = new JwtService(SECRET_B, 3600, 1_209_600);
        String token = other.generateAccessToken(UUID.randomUUID(), "USER");

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLived = new JwtService(SECRET_A, -1, 1_209_600);
        String token = shortLived.generateAccessToken(UUID.randomUUID(), "USER");

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedSignatureIsRejected() {
        String token = service.generateAccessToken(UUID.randomUUID(), "USER");
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtException.class);
    }
}
