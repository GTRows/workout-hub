package com.workouthub.exercises;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.dto.ExerciseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@Tag(name = "Exercises", description = "Public exercise catalog with search and filters.")
@Validated
public class ExerciseController {

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ExerciseDto> list(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Equipment equipment,
            @RequestParam(required = false) Difficulty difficulty,
            @PageableDefault(size = 20, sort = "nameTr") Pageable pageable) {
        return service.list(category, equipment, difficulty, pageable);
    }

    @GetMapping("/{id}")
    public ExerciseDto get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/search")
    public Page<ExerciseDto> search(
            @RequestParam @NotBlank String q,
            @PageableDefault(size = 20, sort = "nameTr") Pageable pageable) {
        return service.search(q, pageable);
    }
}
