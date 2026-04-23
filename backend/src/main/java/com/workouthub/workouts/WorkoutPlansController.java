package com.workouthub.workouts;

import com.workouthub.common.security.AppUserPrincipal;
import com.workouthub.workouts.dto.CreateWorkoutPlanRequest;
import com.workouthub.workouts.dto.UpdateWorkoutPlanRequest;
import com.workouthub.workouts.dto.WorkoutPlanDto;
import com.workouthub.workouts.dto.WorkoutPlanSummaryDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workout-plans")
public class WorkoutPlansController {

    private final WorkoutPlansService service;

    public WorkoutPlansController(WorkoutPlansService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkoutPlanSummaryDto> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return service.list(principal.userId());
    }

    @PostMapping
    public ResponseEntity<WorkoutPlanDto> create(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody CreateWorkoutPlanRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(principal.userId(), req));
    }

    @GetMapping("/{id}")
    public WorkoutPlanDto get(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        return service.get(principal.userId(), id);
    }

    @PutMapping("/{id}")
    public WorkoutPlanDto update(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkoutPlanRequest req) {
        return service.update(principal.userId(), id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        service.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    public WorkoutPlanDto activate(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable UUID id) {
        return service.activate(principal.userId(), id);
    }
}
