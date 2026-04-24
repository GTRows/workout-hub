package com.workouthub.auth;

import com.workouthub.auth.domain.RefreshToken;
import com.workouthub.auth.domain.RefreshTokenRepository;
import com.workouthub.auth.dto.AuthResponse;
import com.workouthub.auth.dto.LoginRequest;
import com.workouthub.auth.dto.RefreshRequest;
import com.workouthub.common.security.JwtService;
import com.workouthub.twofa.TwoFactorService;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TwoFactorService twoFactor;

    public AuthService(
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TwoFactorService twoFactor) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.twoFactor = twoFactor;
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (twoFactor.isEnabled(user.getId())) {
            String code = req.totpCode();
            if (code == null || code.isBlank()
                    || !twoFactor.verifyCode(user.getId(), code)) {
                throw new BadCredentialsException("TOTP code required or invalid");
            }
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshRequest req) {
        Claims claims;
        try {
            claims = jwtService.parse(req.refreshToken());
        } catch (JwtException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!JwtService.TYPE_REFRESH.equals(claims.get("type", String.class))) {
            throw new BadCredentialsException("Not a refresh token");
        }

        String tokenHash = sha256Hex(req.refreshToken());
        RefreshToken stored = refreshTokens.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Refresh token unknown"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);

        UUID userId = UUID.fromString(claims.getSubject());
        User user = users.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refresh = jwtService.generateRefreshToken(user.getId(), user.getRole().name());
        Instant refreshExp = jwtService.parse(refresh).getExpiration().toInstant();

        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(sha256Hex(refresh));
        rt.setExpiresAt(refreshExp);
        rt.setRevoked(false);
        refreshTokens.save(rt);

        return new AuthResponse(access, refresh, user.getId(), user.getRole().name());
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
