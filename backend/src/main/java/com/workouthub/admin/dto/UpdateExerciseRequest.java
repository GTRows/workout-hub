package com.workouthub.admin.dto;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateExerciseRequest(
        @Size(max = 120) String nameTr,
        @Size(max = 120) String nameEn,
        Category category,
        Equipment equipment,
        @Size(max = 40) String musclePrimary,
        @Size(max = 40) String muscleSecondary,
        String descriptionTr,
        List<String> formTips,
        List<String> commonMistakes,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String videoUrl,
        Difficulty difficulty) {}
