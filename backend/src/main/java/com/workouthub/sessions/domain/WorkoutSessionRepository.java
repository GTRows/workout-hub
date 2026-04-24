package com.workouthub.sessions.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByUserIdAndEndedAtIsNull(UUID userId);

    Optional<WorkoutSession> findByIdAndUserId(UUID id, UUID userId);

    Page<WorkoutSession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    List<WorkoutSession> findByUserIdAndEndedAtIsNotNullOrderByStartedAtDesc(UUID userId);

    @Query("""
            SELECT s FROM WorkoutSession s
            WHERE s.userId = :userId
              AND s.endedAt IS NOT NULL
              AND s.startedAt >= :since
            ORDER BY s.startedAt DESC
            """)
    List<WorkoutSession> findFinishedSince(
            @Param("userId") UUID userId,
            @Param("since") Instant since);

    @Modifying
    @Query("UPDATE WorkoutSession s SET s.workoutDayId = NULL WHERE s.userId = :userId")
    int detachSessionsFromDays(@Param("userId") UUID userId);
}
