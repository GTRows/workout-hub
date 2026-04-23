package com.workouthub.admin;

import com.workouthub.users.domain.Role;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a single ADMIN user at boot from APP_ADMIN_EMAIL +
 * APP_ADMIN_PASSWORD_HASH. Public registration is disabled, so this is how
 * the very first admin enters the system. Subsequent users are managed via
 * /api/admin/users by any ADMIN.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final String email;
    private final String passwordHash;
    private final String displayName;

    public AdminSeeder(
            UserRepository users,
            UserProfileRepository profiles,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password-hash:}") String passwordHash,
            @Value("${app.admin.display-name:Admin}") String displayName) {
        this.users = users;
        this.profiles = profiles;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email == null || email.isBlank()
                || passwordHash == null || passwordHash.isBlank()) {
            log.info("Admin seeding skipped: APP_ADMIN_EMAIL or APP_ADMIN_PASSWORD_HASH not set");
            return;
        }
        if (users.existsByEmailIgnoreCase(email)) {
            log.info("Admin seeding skipped: user '{}' already exists", email);
            return;
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordHash);
        admin.setDisplayName(displayName);
        admin.setRole(Role.ADMIN);
        admin = users.save(admin);

        UserProfile profile = new UserProfile();
        profile.setUser(admin);
        profiles.save(profile);

        log.info("Seeded ADMIN user '{}'", email);
    }
}
