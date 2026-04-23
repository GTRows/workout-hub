package com.workouthub.metrics.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BodyMetricRepository extends JpaRepository<BodyMetric, UUID> {

    List<BodyMetric> findByUserIdOrderByRecordedDateDesc(UUID userId);

    Optional<BodyMetric> findByUserIdAndRecordedDate(UUID userId, LocalDate recordedDate);

    Optional<BodyMetric> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT u.id FROM User u
            WHERE NOT EXISTS (
                SELECT 1 FROM BodyMetric m
                WHERE m.userId = u.id AND m.recordedDate >= :since
            )
            """)
    List<UUID> findUserIdsWithoutMetricsSince(@Param("since") LocalDate since);
}
