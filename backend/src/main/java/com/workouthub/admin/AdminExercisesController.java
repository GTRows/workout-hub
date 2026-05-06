package com.workouthub.admin;

import com.workouthub.admin.dto.CreateExerciseRequest;
import com.workouthub.admin.dto.UpdateExerciseRequest;
import com.workouthub.exercises.dto.ExerciseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/exercises")
@Tag(name = "Admin: Exercises", description = "ADMIN-only exercise catalog CRUD.")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExercisesController {

    private final AdminExercisesService service;

    public AdminExercisesController(AdminExercisesService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExerciseDto> create(@Valid @RequestBody CreateExerciseRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ExerciseDto update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExerciseRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
