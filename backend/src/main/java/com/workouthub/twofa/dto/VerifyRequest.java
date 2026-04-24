package com.workouthub.twofa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyRequest(@NotBlank @Size(min = 6, max = 8) String code) {}
