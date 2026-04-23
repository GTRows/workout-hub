package com.workouthub.exercises.domain;

import com.workouthub.common.persistence.LowercaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoryConverter extends LowercaseEnumConverter<Category> {

    public CategoryConverter() {
        super(Category.class);
    }
}
