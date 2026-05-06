package com.workouthub.workouts;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.workouts.domain.WorkoutDayRepository;
import com.workouthub.workouts.dto.WorkoutDayDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Standalone day-by-id lookup. The nested route under /workout-plans is
 * inconvenient from the session execution screen (which only knows
 * workoutDayId from the session, not the plan id). Ownership is enforced
 * by joining through plan.userId in the repository.
 */
@RestController
@RequestMapping("/api/workout-days")
@Tag(name = "Workout Days", description = "Direct workout-day lookup by id, used by the session execution screen.")
public class WorkoutDayByIdController {

    private final WorkoutDayRepository days;

    public WorkoutDayByIdController(WorkoutDayRepository days) {
        this.days = days;
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public WorkoutDayDto get(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        return days.findByIdAndPlanUserId(id, principal.userId())
                .map(WorkoutPlanMapper::toDayDto)
                .orElseThrow(() -> new NotFoundException("Workout day not found: " + id));
    }
}
