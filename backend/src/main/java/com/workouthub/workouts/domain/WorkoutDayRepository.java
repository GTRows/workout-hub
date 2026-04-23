package com.workouthub.workouts.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutDayRepository extends JpaRepository<WorkoutDay, UUID> {

    @Query("SELECT d FROM WorkoutDay d WHERE d.id = :dayId AND d.plan.userId = :userId")
    Optional<WorkoutDay> findByIdAndPlanUserId(
            @Param("dayId") UUID dayId,
            @Param("userId") UUID userId);
}
