package com.workouthub.achievements.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    List<Achievement> findByRuleTypeOrderByThresholdAsc(String ruleType);

    Optional<Achievement> findByCode(String code);
}
