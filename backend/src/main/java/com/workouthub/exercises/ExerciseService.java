package com.workouthub.exercises;

import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.exercises.dto.ExerciseDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExerciseService {

    private final ExerciseRepository repo;

    public ExerciseService(ExerciseRepository repo) {
        this.repo = repo;
    }

    public Page<ExerciseDto> list(
            Category category, Equipment equipment, Difficulty difficulty, Pageable pageable) {
        return repo.search(category, equipment, difficulty, pageable)
                .map(ExerciseMapper::toDto);
    }

    public ExerciseDto get(UUID id) {
        return repo.findById(id)
                .map(ExerciseMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Exercise not found: " + id));
    }

    public Page<ExerciseDto> search(String q, Pageable pageable) {
        return repo.searchByName(q, pageable).map(ExerciseMapper::toDto);
    }
}
