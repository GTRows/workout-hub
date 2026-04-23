package com.workouthub.admin.dto;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateExerciseRequest(
        @NotBlank @Size(max = 120) String nameTr,
        @NotBlank @Size(max = 120) String nameEn,
        @NotNull Category category,
        @NotNull Equipment equipment,
        @NotBlank @Size(max = 40) String musclePrimary,
        @Size(max = 40) String muscleSecondary,
        String descriptionTr,
        List<String> formTips,
        List<String> commonMistakes,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String videoUrl,
        @NotNull Difficulty difficulty) {}
