package com.workouthub.workouts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, UUID> {

    List<WorkoutPlan> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<WorkoutPlan> findByUserIdAndActiveTrue(UUID userId);

    Optional<WorkoutPlan> findByIdAndUserId(UUID id, UUID userId);
}
