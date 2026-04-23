package com.workouthub.push.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubscribeRequest(
        @NotBlank String endpoint,
        @Valid @NotNull Keys keys,
        String userAgent) {

    public record Keys(
            @NotBlank String p256dh,
            @NotBlank String auth) {}
}
