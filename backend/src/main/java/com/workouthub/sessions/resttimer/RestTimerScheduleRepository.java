package com.workouthub.sessions.resttimer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RestTimerScheduleRepository
        extends JpaRepository<RestTimerSchedule, UUID> {

    Optional<RestTimerSchedule> findByUserIdAndSessionId(UUID userId, UUID sessionId);

    @Modifying
    @Transactional
    int deleteByUserIdAndSessionId(UUID userId, UUID sessionId);

    @Query("""
            SELECT r FROM RestTimerSchedule r
            WHERE r.dispatchedAt IS NULL
              AND r.fireAt <= :now
            ORDER BY r.fireAt ASC
            """)
    List<RestTimerSchedule> findDueBatch(
            @Param("now") Instant now,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM RestTimerSchedule r
            WHERE r.dispatchedAt IS NOT NULL
              AND r.dispatchedAt < :cutoff
            """)
    int deleteDispatchedBefore(@Param("cutoff") Instant cutoff);
}
