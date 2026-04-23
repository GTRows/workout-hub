package com.workouthub.supplements.domain;

import com.workouthub.common.persistence.LowercaseEnumConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SupplementTimingConverter extends LowercaseEnumConverter<SupplementTiming> {

    public SupplementTimingConverter() {
        super(SupplementTiming.class);
    }
}
