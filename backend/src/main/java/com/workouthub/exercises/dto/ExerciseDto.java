package com.workouthub.exercises.dto;

import java.util.List;
import java.util.UUID;

public record ExerciseDto(
        UUID id,
        String nameTr,
        String nameEn,
        String category,
        String equipment,
        String musclePrimary,
        String muscleSecondary,
        String descriptionTr,
        List<String> formTips,
        List<String> commonMistakes,
        String imageUrl,
        String videoUrl,
        String difficulty) {}
