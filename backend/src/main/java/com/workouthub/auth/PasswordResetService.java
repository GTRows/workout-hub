package com.workouthub.auth;

import com.workouthub.auth.domain.PasswordResetToken;
import com.workouthub.auth.domain.PasswordResetTokenRepository;
import com.workouthub.auth.domain.RefreshTokenRepository;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PasswordResetService {

    private static final int TTL_HOURS = 24;
    private static final int TOKEN_RAW_BYTES = 32;
    private static final SecureRandom RNG = new SecureRandom();

    private final PasswordResetTokenRepository tokens;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public PasswordResetService(
            PasswordResetTokenRepository tokens,
            UserRepository users,
            RefreshTokenRepository refreshTokens,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.tokens = tokens;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * Admin generates a single-use token for a target user. The token is
     * returned in plaintext only once; only its SHA-256 hash is stored.
     */
    public String issueFor(UUID targetUserId, UUID adminId) {
        User target = users.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));

        String plaintext = generateToken();
        PasswordResetToken row = new PasswordResetToken();
        row.setUserId(target.getId());
        row.setTokenHash(sha256(plaintext));
        row.setIssuedBy(adminId);
        row.setExpiresAt(Instant.now(clock).plus(TTL_HOURS, ChronoUnit.HOURS));
        tokens.save(row);
        return plaintext;
    }

    public void consume(String plaintext, String newPassword) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "new password must be at least 8 characters");
        }

        PasswordResetToken row = tokens.findByTokenHash(sha256(plaintext))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "invalid or already-consumed token"));

        if (row.getConsumedAt() != null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "invalid or already-consumed token");
        }
        if (row.getExpiresAt().isBefore(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "token expired");
        }

        User user = users.findById(row.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "target user not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);

        row.setConsumedAt(Instant.now(clock));
        tokens.save(row);

        // Force re-login on all existing sessions for the target user.
        refreshTokens.revokeAllForUser(user.getId());
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_RAW_BYTES];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
