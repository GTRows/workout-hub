package com.workouthub.webhooks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Smart-scale relays differ on field names; this record accepts the
 * common variants so the same webhook URL works for IFTTT, Apple
 * Shortcuts, Home Assistant, etc., without per-source mapping.
 */
public record ScalePayload(
        @JsonProperty("weightKg")
        @JsonAlias({"weight", "value", "kg"})
        BigDecimal weightKg,

        @JsonProperty("timestamp")
        @JsonAlias({"ts", "recordedAt", "date", "datetime"})
        Instant timestamp) {}
