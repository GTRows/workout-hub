package com.workouthub.support;

import com.workouthub.common.security.JwtService;
import com.workouthub.users.domain.Role;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test-scoped helper (lives under src/test) that seeds users directly via
 * repositories -- bypassing HTTP endpoints. Used because public registration
 * is disabled; integration tests need a way to materialize users without
 * going through the admin API surface in every test.
 */
@Component
public class TestAuthHelpers {

    @Autowired UserRepository users;
    @Autowired UserProfileRepository profiles;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    public record SeededUser(UUID id, String email, String accessToken) {}

    @Transactional
    public SeededUser seed(String email, String plain, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plain));
        user.setDisplayName("Seeded " + email);
        user.setRole(role);
        user = users.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profiles.save(profile);

        String access = jwtService.generateAccessToken(user.getId(), role.name());
        return new SeededUser(user.getId(), email, access);
    }
}
