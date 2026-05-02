package com.workouthub.supplements.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum SupplementTiming {
    MORNING,
    PRE_WORKOUT,
    POST_WORKOUT,
    EVENING,
    WITH_MEAL,
    OTHER;

    @JsonValue
    public String toJson() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static SupplementTiming fromJson(String value) {
        if (value == null) return OTHER;
        return SupplementTiming.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
