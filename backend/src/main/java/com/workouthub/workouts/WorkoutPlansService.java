package com.workouthub.workouts;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import com.workouthub.workouts.dto.CreateWorkoutPlanRequest;
import com.workouthub.workouts.dto.UpdateWorkoutPlanRequest;
import com.workouthub.workouts.dto.WorkoutPlanDto;
import com.workouthub.workouts.dto.WorkoutPlanSummaryDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkoutPlansService {

    private final WorkoutPlanRepository plans;

    public WorkoutPlansService(WorkoutPlanRepository plans) {
        this.plans = plans;
    }

    @Transactional(readOnly = true)
    public List<WorkoutPlanSummaryDto> list(UUID userId) {
        return plans.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(WorkoutPlanMapper::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkoutPlanDto get(UUID userId, UUID planId) {
        return WorkoutPlanMapper.toDto(findOwnedOrThrow(userId, planId));
    }

    @Transactional(readOnly = true)
    public Optional<WorkoutPlanDto> getActive(UUID userId) {
        return plans.findByUserIdAndActiveTrue(userId).map(WorkoutPlanMapper::toDto);
    }

    public WorkoutPlanDto create(UUID userId, CreateWorkoutPlanRequest req) {
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setName(req.name());
        plan.setActive(false);
        return WorkoutPlanMapper.toDto(plans.save(plan));
    }

    public WorkoutPlanDto update(UUID userId, UUID planId, UpdateWorkoutPlanRequest req) {
        WorkoutPlan plan = findOwnedOrThrow(userId, planId);
        if (req.name() != null) plan.setName(req.name());
        return WorkoutPlanMapper.toDto(plan);
    }

    public void delete(UUID userId, UUID planId) {
        WorkoutPlan plan = findOwnedOrThrow(userId, planId);
        plans.delete(plan);
    }

    public WorkoutPlanDto activate(UUID userId, UUID planId) {
        WorkoutPlan target = findOwnedOrThrow(userId, planId);
        if (target.isActive()) {
            return WorkoutPlanMapper.toDto(target);
        }
        // The partial unique index on workout_plans (user_id) WHERE is_active
        // forbids two active rows at any moment, so flush the deactivate
        // before setting the new active plan.
        plans.findByUserIdAndActiveTrue(userId).ifPresent(current -> {
            current.setActive(false);
            plans.save(current);
        });
        plans.flush();
        target.setActive(true);
        return WorkoutPlanMapper.toDto(plans.save(target));
    }

    WorkoutPlan findOwnedOrThrow(UUID userId, UUID planId) {
        return plans.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new NotFoundException("Workout plan not found: " + planId));
    }
}
