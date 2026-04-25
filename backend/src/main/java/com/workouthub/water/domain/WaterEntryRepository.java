package com.workouthub.water.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterEntryRepository extends JpaRepository<WaterEntry, UUID> {

    List<WaterEntry> findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc(
            UUID userId, Instant from, Instant to);

    Optional<WaterEntry> findByIdAndUserId(UUID id, UUID userId);
}
