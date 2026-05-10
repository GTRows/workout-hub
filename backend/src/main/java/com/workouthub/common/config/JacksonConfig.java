package com.workouthub.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    // Jackson 3 ships JavaTimeModule built into jackson-databind and defaults
    // WRITE_DATES_AS_TIMESTAMPS to false (DateTimeFeature in Jackson 3), so the
    // module-add and feature-disable from Jackson 2 are no longer needed.
    // ObjectMapper is now immutable; serializationInclusion was replaced by
    // changeDefaultPropertyInclusion on the builder.
    @Bean
    JsonMapperBuilderCustomizer jacksonBuilderCustomizer() {
        return builder -> builder.changeDefaultPropertyInclusion(
                incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL));
    }
}
