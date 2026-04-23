package com.workouthub.sessions.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionSetRepository extends JpaRepository<SessionSet, UUID> {

    Optional<SessionSet> findByIdAndSessionId(UUID id, UUID sessionId);

    List<SessionSet> findBySessionIdAndExerciseIdOrderBySetNumberAsc(
            UUID sessionId, UUID exerciseId);

    int countBySessionIdAndExerciseId(UUID sessionId, UUID exerciseId);

    @Query("""
            SELECT s FROM SessionSet s
            WHERE s.session.userId = :userId
              AND s.exercise.id   = :exerciseId
              AND s.session.endedAt IS NOT NULL
            ORDER BY s.session.startedAt DESC, s.setNumber ASC
            """)
    List<SessionSet> findHistoricalByUserAndExercise(
            @Param("userId") UUID userId,
            @Param("exerciseId") UUID exerciseId);
}
