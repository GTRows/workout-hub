package com.workouthub.workouts;

import com.workouthub.common.web.ConflictException;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.workouts.domain.WorkoutDay;
import com.workouthub.workouts.domain.WorkoutDayExercise;
import com.workouthub.workouts.domain.WorkoutPlan;
import com.workouthub.workouts.domain.WorkoutPlanRepository;
import com.workouthub.workouts.dto.AddDayExerciseRequest;
import com.workouthub.workouts.dto.CreateWorkoutDayRequest;
import com.workouthub.workouts.dto.UpdateDayExerciseRequest;
import com.workouthub.workouts.dto.UpdateWorkoutDayRequest;
import com.workouthub.workouts.dto.WorkoutDayDto;
import com.workouthub.workouts.dto.WorkoutDayExerciseDto;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkoutDaysService {

    private final WorkoutPlansService plansService;
    private final WorkoutPlanRepository plans;
    private final ExerciseRepository exerciseRepo;

    public WorkoutDaysService(
            WorkoutPlansService plansService,
            WorkoutPlanRepository plans,
            ExerciseRepository exerciseRepo) {
        this.plansService = plansService;
        this.plans = plans;
        this.exerciseRepo = exerciseRepo;
    }

    public WorkoutDayDto createDay(UUID userId, UUID planId, CreateWorkoutDayRequest req) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        if (hasDayOfWeek(plan, req.dayOfWeek(), null)) {
            throw new ConflictException("Day " + req.dayOfWeek() + " already exists on this plan");
        }
        WorkoutDay day = new WorkoutDay();
        day.setDayOfWeek(req.dayOfWeek());
        day.setName(req.name());
        day.setFocus(req.focus());
        day.setEstimatedDurationMin(req.estimatedDurationMin());
        plan.addDay(day);
        plans.saveAndFlush(plan);
        return WorkoutPlanMapper.toDayDto(day);
    }

    public WorkoutDayDto updateDay(
            UUID userId, UUID planId, UUID dayId, UpdateWorkoutDayRequest req) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        WorkoutDay day = findDayOrThrow(plan, dayId);
        if (req.dayOfWeek() != null && req.dayOfWeek() != day.getDayOfWeek()) {
            if (hasDayOfWeek(plan, req.dayOfWeek(), dayId)) {
                throw new ConflictException("Day " + req.dayOfWeek() + " already exists on this plan");
            }
            day.setDayOfWeek(req.dayOfWeek());
        }
        if (req.name() != null) day.setName(req.name());
        if (req.focus() != null) day.setFocus(req.focus());
        if (req.estimatedDurationMin() != null) day.setEstimatedDurationMin(req.estimatedDurationMin());
        return WorkoutPlanMapper.toDayDto(day);
    }

    public void deleteDay(UUID userId, UUID planId, UUID dayId) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        WorkoutDay day = findDayOrThrow(plan, dayId);
        plan.removeDay(day);
    }

    public WorkoutDayExerciseDto addItem(
            UUID userId, UUID planId, UUID dayId, AddDayExerciseRequest req) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        WorkoutDay day = findDayOrThrow(plan, dayId);
        Exercise exercise = exerciseRepo.findById(req.exerciseId())
                .orElseThrow(() -> new NotFoundException("Exercise not found: " + req.exerciseId()));

        WorkoutDayExercise item = new WorkoutDayExercise();
        item.setExercise(exercise);
        item.setOrderIndex(day.getExercises().size() + 1);
        item.setTargetSets(req.targetSets());
        item.setTargetRepsMin(req.targetRepsMin());
        item.setTargetRepsMax(req.targetRepsMax());
        item.setTargetWeightKg(req.targetWeightKg());
        item.setRestSeconds(req.restSeconds());
        item.setNotes(req.notes());
        day.addExercise(item);
        plans.saveAndFlush(plan);
        return WorkoutPlanMapper.toItemDto(item);
    }

    public WorkoutDayExerciseDto updateItem(
            UUID userId, UUID planId, UUID dayId, UUID itemId, UpdateDayExerciseRequest req) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        WorkoutDay day = findDayOrThrow(plan, dayId);
        WorkoutDayExercise item = findItemOrThrow(day, itemId);
        if (req.targetSets() != null) item.setTargetSets(req.targetSets());
        if (req.targetRepsMin() != null) item.setTargetRepsMin(req.targetRepsMin());
        if (req.targetRepsMax() != null) item.setTargetRepsMax(req.targetRepsMax());
        if (req.targetWeightKg() != null) item.setTargetWeightKg(req.targetWeightKg());
        if (req.restSeconds() != null) item.setRestSeconds(req.restSeconds());
        if (req.notes() != null) item.setNotes(req.notes());
        return WorkoutPlanMapper.toItemDto(item);
    }

    public void deleteItem(UUID userId, UUID planId, UUID dayId, UUID itemId) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        WorkoutDay day = findDayOrThrow(plan, dayId);
        WorkoutDayExercise item = findItemOrThrow(day, itemId);
        day.removeExercise(item);
        plans.saveAndFlush(plan); // flush the orphan delete before renumbering

        int idx = 1;
        for (WorkoutDayExercise remaining : day.getExercises()) {
            remaining.setOrderIndex(idx++);
        }
    }

    public WorkoutDayDto reorderItems(
            UUID userId, UUID planId, UUID dayId, List<UUID> itemIdsInOrder) {
        WorkoutPlan plan = plansService.findOwnedOrThrow(userId, planId);
        WorkoutDay day = findDayOrThrow(plan, dayId);
        List<WorkoutDayExercise> items = day.getExercises();

        var existingIds = items.stream().map(WorkoutDayExercise::getId).collect(Collectors.toSet());
        if (items.size() != itemIdsInOrder.size()
                || !existingIds.equals(new HashSet<>(itemIdsInOrder))) {
            throw new ConflictException(
                    "Reorder list must contain every existing item exactly once");
        }

        // Two-phase to dodge the UNIQUE (workout_day_id, order_index) constraint
        // on intermediate states: first flip all to negative sentinels, flush,
        // then assign positive target indices.
        int neg = -1;
        for (WorkoutDayExercise item : items) {
            item.setOrderIndex(neg--);
        }
        plans.saveAndFlush(plan);

        for (int i = 0; i < itemIdsInOrder.size(); i++) {
            UUID id = itemIdsInOrder.get(i);
            WorkoutDayExercise target = items.stream()
                    .filter(x -> x.getId().equals(id))
                    .findFirst()
                    .orElseThrow();
            target.setOrderIndex(i + 1);
        }
        plans.saveAndFlush(plan);

        return WorkoutPlanMapper.toDayDto(day);
    }

    private static boolean hasDayOfWeek(WorkoutPlan plan, Short dayOfWeek, UUID excludingDayId) {
        return plan.getDays().stream().anyMatch(d ->
                d.getDayOfWeek() == dayOfWeek
                        && (excludingDayId == null || !d.getId().equals(excludingDayId)));
    }

    private static WorkoutDay findDayOrThrow(WorkoutPlan plan, UUID dayId) {
        return plan.getDays().stream()
                .filter(d -> d.getId().equals(dayId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Workout day not found: " + dayId));
    }

    private static WorkoutDayExercise findItemOrThrow(WorkoutDay day, UUID itemId) {
        return day.getExercises().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Day exercise not found: " + itemId));
    }
}
