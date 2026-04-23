package com.workouthub.common.persistence;

import jakarta.persistence.AttributeConverter;

/**
 * Base converter that stores enum constants as lowercase strings. Multiple
 * domain enums (exercise Category, Equipment, Difficulty; workout Focus;
 * ...) persist with lowercase CHECK-constrained values, so we keep the
 * Java names in uppercase convention and convert at the JPA boundary.
 */
public abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected LowercaseEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.name().toLowerCase();
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        return dbValue == null ? null : Enum.valueOf(type, dbValue.toUpperCase());
    }
}
