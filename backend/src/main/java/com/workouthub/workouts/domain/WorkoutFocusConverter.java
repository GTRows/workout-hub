package com.workouthub.workouts.domain;

import com.workouthub.common.persistence.LowercaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WorkoutFocusConverter extends LowercaseEnumConverter<WorkoutFocus> {

    public WorkoutFocusConverter() {
        super(WorkoutFocus.class);
    }
}
