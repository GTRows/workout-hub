package com.workouthub.common.config;

import com.workouthub.exercises.domain.Category;
import com.workouthub.exercises.domain.Difficulty;
import com.workouthub.exercises.domain.Equipment;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Query-parameter binding for catalog enums is case-insensitive: the public
 * API contract carries lowercase strings ("push", "dumbbell", "beginner")
 * to match the DB CHECK constraints in V3, while the Java enums stay
 * uppercase to follow convention.
 */
@Configuration
public class WebConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Category.class,
                source -> Category.valueOf(source.toUpperCase()));
        registry.addConverter(String.class, Equipment.class,
                source -> Equipment.valueOf(source.toUpperCase()));
        registry.addConverter(String.class, Difficulty.class,
                source -> Difficulty.valueOf(source.toUpperCase()));
    }
}
