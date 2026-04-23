package com.workouthub.admin;

import com.workouthub.admin.dto.CreateExerciseRequest;
import com.workouthub.admin.dto.UpdateExerciseRequest;
import com.workouthub.common.web.ConflictException;
import com.workouthub.common.web.NotFoundException;
import com.workouthub.exercises.ExerciseMapper;
import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.domain.ExerciseRepository;
import com.workouthub.exercises.dto.ExerciseDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminExercisesService {

    private final ExerciseRepository repo;

    public AdminExercisesService(ExerciseRepository repo) {
        this.repo = repo;
    }

    public ExerciseDto create(CreateExerciseRequest req) {
        if (repo.existsByNameEnIgnoreCase(req.nameEn())) {
            throw new ConflictException("Exercise with this English name already exists");
        }
        Exercise e = new Exercise();
        e.setNameTr(req.nameTr());
        e.setNameEn(req.nameEn());
        e.setCategory(req.category());
        e.setEquipment(req.equipment());
        e.setMusclePrimary(req.musclePrimary());
        e.setMuscleSecondary(req.muscleSecondary());
        e.setDescriptionTr(req.descriptionTr());
        e.setDescriptionEn(req.descriptionEn());
        e.setFormTipsTr(copyOrEmpty(req.formTipsTr()));
        e.setFormTipsEn(copyOrEmpty(req.formTipsEn()));
        e.setCommonMistakesTr(copyOrEmpty(req.commonMistakesTr()));
        e.setCommonMistakesEn(copyOrEmpty(req.commonMistakesEn()));
        e.setImageUrl(req.imageUrl());
        e.setVideoUrl(req.videoUrl());
        e.setDifficulty(req.difficulty());
        return ExerciseMapper.toDto(repo.save(e));
    }

    public ExerciseDto update(UUID id, UpdateExerciseRequest req) {
        Exercise e = findOrThrow(id);
        if (req.nameTr() != null) e.setNameTr(req.nameTr());
        if (req.nameEn() != null) {
            if (!req.nameEn().equalsIgnoreCase(e.getNameEn())
                    && repo.existsByNameEnIgnoreCase(req.nameEn())) {
                throw new ConflictException("Exercise with this English name already exists");
            }
            e.setNameEn(req.nameEn());
        }
        if (req.category() != null) e.setCategory(req.category());
        if (req.equipment() != null) e.setEquipment(req.equipment());
        if (req.musclePrimary() != null) e.setMusclePrimary(req.musclePrimary());
        if (req.muscleSecondary() != null) e.setMuscleSecondary(req.muscleSecondary());
        if (req.descriptionTr() != null) e.setDescriptionTr(req.descriptionTr());
        if (req.descriptionEn() != null) e.setDescriptionEn(req.descriptionEn());
        if (req.formTipsTr() != null) e.setFormTipsTr(new ArrayList<>(req.formTipsTr()));
        if (req.formTipsEn() != null) e.setFormTipsEn(new ArrayList<>(req.formTipsEn()));
        if (req.commonMistakesTr() != null) e.setCommonMistakesTr(new ArrayList<>(req.commonMistakesTr()));
        if (req.commonMistakesEn() != null) e.setCommonMistakesEn(new ArrayList<>(req.commonMistakesEn()));
        if (req.imageUrl() != null) e.setImageUrl(req.imageUrl());
        if (req.videoUrl() != null) e.setVideoUrl(req.videoUrl());
        if (req.difficulty() != null) e.setDifficulty(req.difficulty());
        return ExerciseMapper.toDto(e);
    }

    public void delete(UUID id) {
        Exercise existing = findOrThrow(id);
        try {
            repo.delete(existing);
            repo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Exercise is referenced by a workout plan and cannot be deleted");
        }
    }

    private Exercise findOrThrow(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Exercise not found: " + id));
    }

    private static List<String> copyOrEmpty(List<String> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }
}
