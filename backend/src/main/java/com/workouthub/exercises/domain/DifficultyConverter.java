package com.workouthub.exercises.domain;

import com.workouthub.common.persistence.LowercaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DifficultyConverter extends LowercaseEnumConverter<Difficulty> {

    public DifficultyConverter() {
        super(Difficulty.class);
    }
}
