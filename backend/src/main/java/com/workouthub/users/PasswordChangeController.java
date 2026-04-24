package com.workouthub.users;

import com.workouthub.auth.domain.RefreshTokenRepository;
import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users/me")
public class PasswordChangeController {

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8) String newPassword) {}

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokens;

    public PasswordChangeController(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
    }

    @PutMapping("/password")
    @Transactional
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest req) {
        User user = users.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "user not found"));

        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "current password does not match");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        users.save(user);

        // Force re-login on every device that currently holds a refresh
        // token for this user, including the caller's own session.
        refreshTokens.revokeAllForUser(user.getId());
        return ResponseEntity.noContent().build();
    }
}
