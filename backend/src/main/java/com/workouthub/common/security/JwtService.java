package com.workouthub.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${app.jwt.refresh-ttl-seconds}") long refreshTtlSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
    }

    public String generateAccessToken(UUID userId, String role) {
        return build(userId, role, TYPE_ACCESS, accessTtlSeconds);
    }

    public String generateRefreshToken(UUID userId, String role) {
        return build(userId, role, TYPE_REFRESH, refreshTtlSeconds);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String build(UUID userId, String role, String type, long ttlSeconds) {
        Instant now = Instant.now();
        Instant expires = now.plus(ttlSeconds, ChronoUnit.SECONDS);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expires))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
