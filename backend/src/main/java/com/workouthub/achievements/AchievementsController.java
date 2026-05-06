package com.workouthub.achievements;

import com.workouthub.achievements.domain.Achievement;
import com.workouthub.achievements.domain.AchievementRepository;
import com.workouthub.achievements.domain.UserAchievement;
import com.workouthub.achievements.domain.UserAchievementRepository;
import com.workouthub.achievements.dto.AchievementDto;
import com.workouthub.common.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
@Tag(name = "Achievements", description = "User-earned achievement badges and progression milestones.")
public class AchievementsController {

    private final AchievementRepository achievements;
    private final UserAchievementRepository unlocks;

    public AchievementsController(
            AchievementRepository achievements,
            UserAchievementRepository unlocks) {
        this.achievements = achievements;
        this.unlocks = unlocks;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public List<AchievementDto> mine(@AuthenticationPrincipal AppUserPrincipal principal) {
        Map<UUID, UserAchievement> byAchievementId = new HashMap<>();
        for (UserAchievement ua : unlocks.findByUserIdOrderByUnlockedAtDesc(principal.userId())) {
            byAchievementId.put(ua.getAchievementId(), ua);
        }
        return achievements.findAll().stream()
                .map(def -> toDto(def, byAchievementId.get(def.getId())))
                .sorted((a, b) -> {
                    if (a.unlocked() != b.unlocked()) return a.unlocked() ? -1 : 1;
                    return Integer.compare(a.threshold(), b.threshold());
                })
                .toList();
    }

    private AchievementDto toDto(Achievement def, UserAchievement ua) {
        return new AchievementDto(
                def.getId(),
                def.getCode(),
                def.getNameTr(),
                def.getNameEn(),
                def.getDescriptionTr(),
                def.getDescriptionEn(),
                def.getIcon(),
                def.getRuleType(),
                def.getThreshold(),
                ua != null,
                ua == null ? null : ua.getUnlockedAt(),
                ua == null ? null : ua.getProgressValue());
    }
}
