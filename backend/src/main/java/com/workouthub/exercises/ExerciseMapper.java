package com.workouthub.exercises;

import com.workouthub.exercises.domain.Exercise;
import com.workouthub.exercises.dto.ExerciseDto;

public final class ExerciseMapper {

    private ExerciseMapper() {}

    public static ExerciseDto toDto(Exercise e) {
        return new ExerciseDto(
                e.getId(),
                e.getNameTr(),
                e.getNameEn(),
                e.getCategory().name().toLowerCase(),
                e.getEquipment().name().toLowerCase(),
                e.getMusclePrimary(),
                e.getMuscleSecondary(),
                e.getDescriptionTr(),
                e.getDescriptionEn(),
                e.getFormTipsTr(),
                e.getFormTipsEn(),
                e.getCommonMistakesTr(),
                e.getCommonMistakesEn(),
                e.getImageUrl(),
                e.getVideoUrl(),
                e.getDifficulty().name().toLowerCase());
    }
}
