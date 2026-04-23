package com.workouthub.exercises.domain;

import com.workouthub.common.persistence.LowercaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EquipmentConverter extends LowercaseEnumConverter<Equipment> {

    public EquipmentConverter() {
        super(Equipment.class);
    }
}
