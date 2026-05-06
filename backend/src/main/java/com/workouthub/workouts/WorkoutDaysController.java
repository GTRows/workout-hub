package com.workouthub.workouts;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.workouts.dto.AddDayExerciseRequest;
import com.workouthub.workouts.dto.CreateWorkoutDayRequest;
import com.workouthub.workouts.dto.ReorderDayExercisesRequest;
import com.workouthub.workouts.dto.UpdateDayExerciseRequest;
import com.workouthub.workouts.dto.UpdateWorkoutDayRequest;
import com.workouthub.workouts.dto.WorkoutDayDto;
import com.workouthub.workouts.dto.WorkoutDayExerciseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workout-plans/{planId}")
@Tag(name = "Workout Plans: Days", description = "Day CRUD nested under a parent workout plan.")
public class WorkoutDaysController {

    private final WorkoutDaysService service;

    public WorkoutDaysController(WorkoutDaysService service) {
        this.service = service;
    }

    @PostMapping("/days")
    public ResponseEntity<WorkoutDayDto> createDay(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @Valid @RequestBody CreateWorkoutDayRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDay(principal.userId(), planId, req));
    }

    @PutMapping("/days/{dayId}")
    public WorkoutDayDto updateDay(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @Valid @RequestBody UpdateWorkoutDayRequest req) {
        return service.updateDay(principal.userId(), planId, dayId, req);
    }

    @DeleteMapping("/days/{dayId}")
    public ResponseEntity<Void> deleteDay(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @PathVariable UUID dayId) {
        service.deleteDay(principal.userId(), planId, dayId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/days/{dayId}/exercises")
    public ResponseEntity<WorkoutDayExerciseDto> addItem(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @Valid @RequestBody AddDayExerciseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.addItem(principal.userId(), planId, dayId, req));
    }

    @PutMapping("/days/{dayId}/exercises/{itemId}")
    public WorkoutDayExerciseDto updateItem(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateDayExerciseRequest req) {
        return service.updateItem(principal.userId(), planId, dayId, itemId, req);
    }

    @DeleteMapping("/days/{dayId}/exercises/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @PathVariable UUID itemId) {
        service.deleteItem(principal.userId(), planId, dayId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/days/{dayId}/exercises/reorder")
    public WorkoutDayDto reorder(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID planId,
            @PathVariable UUID dayId,
            @Valid @RequestBody ReorderDayExercisesRequest req) {
        return service.reorderItems(principal.userId(), planId, dayId, req.itemIdsInOrder());
    }
}
