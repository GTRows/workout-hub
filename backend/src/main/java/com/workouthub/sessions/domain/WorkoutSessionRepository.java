package com.workouthub.sessions.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, UUID> {

    Optional<WorkoutSession> findByUserIdAndEndedAtIsNull(UUID userId);

    Optional<WorkoutSession> findByIdAndUserId(UUID id, UUID userId);

    Page<WorkoutSession> findByUserIdOrderByStartedAtDesc(UUID userId, Pageable pageable);

    List<WorkoutSession> findByUserIdAndEndedAtIsNotNullOrderByStartedAtDesc(UUID userId);
}
