package com.workouthub.users;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.users.domain.User;
import com.workouthub.users.domain.UserProfile;
import com.workouthub.users.domain.UserProfileRepository;
import com.workouthub.users.domain.UserRepository;
import com.workouthub.users.dto.UpdateProfileRequest;
import com.workouthub.users.dto.UserMeResponse;
import com.workouthub.users.dto.UserProfileDto;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UsersService {

    private final UserRepository users;
    private final UserProfileRepository profiles;

    public UsersService(UserRepository users, UserProfileRepository profiles) {
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMe(UUID userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        UserProfile profile = profiles.findById(userId)
                .orElseGet(() -> attachEmptyProfile(user));
        return toResponse(user, profile);
    }

    public UserMeResponse updateMe(UUID userId, UpdateProfileRequest req) {
        User user = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (req.displayName() != null) {
            user.setDisplayName(req.displayName());
        }

        UserProfile profile = profiles.findById(userId)
                .orElseGet(() -> attachEmptyProfile(user));

        if (req.heightCm() != null) profile.setHeightCm(req.heightCm());
        if (req.weightKg() != null) profile.setWeightKg(req.weightKg());
        if (req.birthDate() != null) profile.setBirthDate(req.birthDate());
        if (req.gender() != null) profile.setGender(req.gender());
        if (req.healthNotes() != null) profile.setHealthNotes(req.healthNotes());
        if (req.goals() != null) profile.setGoals(req.goals());
        if (req.dailyKcalGoal() != null) profile.setDailyKcalGoal(req.dailyKcalGoal());
        if (req.dailyProteinGGoal() != null) profile.setDailyProteinGGoal(req.dailyProteinGGoal());
        if (req.dailyCarbsGGoal() != null) profile.setDailyCarbsGGoal(req.dailyCarbsGGoal());
        if (req.dailyFatGGoal() != null) profile.setDailyFatGGoal(req.dailyFatGGoal());

        profiles.save(profile);
        return toResponse(user, profile);
    }

    private UserProfile attachEmptyProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        return profiles.save(profile);
    }

    private UserMeResponse toResponse(User user, UserProfile profile) {
        return new UserMeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                new UserProfileDto(
                        profile.getHeightCm(),
                        profile.getWeightKg(),
                        profile.getBirthDate(),
                        profile.getGender(),
                        profile.getHealthNotes(),
                        profile.getGoals(),
                        profile.getDailyKcalGoal(),
                        profile.getDailyProteinGGoal(),
                        profile.getDailyCarbsGGoal(),
                        profile.getDailyFatGGoal()));
    }
}
