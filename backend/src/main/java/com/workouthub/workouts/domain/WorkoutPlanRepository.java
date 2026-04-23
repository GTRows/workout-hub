package com.workouthub.workouts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {

    List<WorkoutPlan> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<WorkoutPlan> findByUserIdAndActiveTrue(UUID userId);

    Optional<WorkoutPlan> findByIdAndUserId(UUID id, UUID userId);

    @Query("""
            SELECT DISTINCT p.userId FROM WorkoutPlan p JOIN p.days d
            WHERE p.active = true AND d.dayOfWeek = :dayOfWeek
            """)
    List<UUID> findUserIdsWithActiveWorkoutOnDay(@Param("dayOfWeek") short dayOfWeek);
}
