package com.workouthub.exercises.domain;

import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EquipmentConverter extends LowercaseEnumConverter<Equipment> {

    public EquipmentConverter() {
        super(Equipment.class);
    }
}
