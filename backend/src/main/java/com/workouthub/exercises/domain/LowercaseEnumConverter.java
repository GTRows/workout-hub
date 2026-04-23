package com.workouthub.exercises.domain;

import jakarta.persistence.AttributeConverter;

/**
 * Base converter that stores enum constants as lowercase strings. Schema
 * CHECK constraints in V3 were written in lowercase ("push", "pull", ...)
 * so enum.name().toLowerCase() is the canonical DB form.
 */
abstract class LowercaseEnumConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

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
