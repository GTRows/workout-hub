package com.workouthub.exercises.dto;

import java.util.List;
import java.util.UUID;

/**
 * Full bilingual exercise payload. Clients pick the locale side they need;
 * the server never filters here so the /users/me language preference can
 * stay client-side until a third locale appears.
 */
public record ExerciseDto(
        UUID id,
        String nameTr,
        String nameEn,
        String category,
        String equipment,
        String musclePrimary,
        String muscleSecondary,
        String descriptionTr,
        String descriptionEn,
        List<String> formTipsTr,
        List<String> formTipsEn,
        List<String> commonMistakesTr,
        List<String> commonMistakesEn,
        String imageUrl,
        String videoUrl,
        String difficulty) {}
