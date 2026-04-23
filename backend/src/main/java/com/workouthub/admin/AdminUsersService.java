package com.workouthub.admin;

import com.workouthub.admin.dto.AdminUserResponse;
import com.workouthub.admin.dto.CreateUserRequest;
import com.workouthub.admin.dto.UpdateUserRequest;
import com.workouthub.common.web.ConflictException;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminUsersService {

    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final PasswordEncoder passwordEncoder;

    public AdminUsersService(
            UserRepository users,
            UserProfileRepository profiles,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.profiles = profiles;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminUserResponse create(CreateUserRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email already registered");
        }
        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setDisplayName(req.displayName());
        user.setRole(req.role());
        user = users.save(user);

        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profiles.save(profile);

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return users.findAll(Sort.by("email")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public AdminUserResponse update(UUID id, UpdateUserRequest req) {
        User user = findOrThrow(id);
        if (req.displayName() != null) {
            user.setDisplayName(req.displayName());
        }
        if (req.role() != null) {
            user.setRole(req.role());
        }
        return toResponse(users.save(user));
    }

    public void delete(UUID id, UUID callerId) {
        if (id.equals(callerId)) {
            throw new ConflictException("Cannot delete your own account");
        }
        if (!users.existsById(id)) {
            throw new NotFoundException("User not found");
        }
        users.deleteById(id);
    }

    private User findOrThrow(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
