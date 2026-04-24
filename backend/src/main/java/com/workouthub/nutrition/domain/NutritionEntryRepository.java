package com.workouthub.nutrition.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionEntryRepository extends JpaRepository<NutritionEntry, UUID> {

    List<NutritionEntry> findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
            UUID userId, Instant from, Instant to);

    Optional<NutritionEntry> findByIdAndUserId(UUID id, UUID userId);
}
