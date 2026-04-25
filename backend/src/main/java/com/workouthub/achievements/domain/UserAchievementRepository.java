package com.workouthub.achievements.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(UUID userId);

    boolean existsByUserIdAndAchievementId(UUID userId, UUID achievementId);
}
