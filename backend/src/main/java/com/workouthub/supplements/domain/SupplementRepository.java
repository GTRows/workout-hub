package com.workouthub.supplements.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplementRepository extends JpaRepository<Supplement, UUID> {

    List<Supplement> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<Supplement> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT s FROM Supplement s
            WHERE s.active = true
              AND s.reminderTime IS NOT NULL
              AND s.reminderTime BETWEEN :from AND :to
            """)
    List<Supplement> findActiveWithReminderBetween(
            @Param("from") LocalTime from,
            @Param("to") LocalTime to);
}
