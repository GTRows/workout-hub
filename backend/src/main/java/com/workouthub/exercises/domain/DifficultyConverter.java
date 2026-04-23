package com.workouthub.exercises.domain;

import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DifficultyConverter extends LowercaseEnumConverter<Difficulty> {

    public DifficultyConverter() {
        super(Difficulty.class);
    }
}
