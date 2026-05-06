package com.workouthub.common.web;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(
        description =
                "Project-wide error envelope returned for every 4xx and 5xx response. "
                        + "Field `code` carries a typed enum on conflict responses that map to known "
                        + "business-rule violations.")
public record ApiError(
        @Schema(description = "Server-side wall-clock instant when the error response was produced.")
                Instant timestamp,
        @Schema(description = "HTTP status code (numeric, mirrors the response status line).") int status,
        @Schema(description = "HTTP status reason phrase (human-readable, mirrors the response status line).")
                String error,
        @Schema(
                        description =
                                "Human-readable error message. Not stable across releases; tooling should branch on `status` and `code`, not `message`.")
                String message,
        @Schema(description = "Request URI that produced the error.") String path,
        @Schema(
                        description =
                                "Field-level validation errors when the response is a 400 from request-DTO validation. Null otherwise.")
                List<FieldError> errors,
        @Schema(
                        description =
                                "Typed error code populated for conflict responses tied to known business rules. May be null on legacy paths.",
                        nullable = true,
                        allowableValues = {
                            "SESSION_ALREADY_ACTIVE",
                            "SESSION_ALREADY_FINISHED",
                            "SESSION_FINISHED",
                            "SET_NUMBER_DUPLICATE"
                        })
                String code) {

    @Schema(
            description =
                    "One field-level validation error. Populated by 400 responses when request DTO validation fails.")
    public record FieldError(
            @Schema(description = "Request DTO property path that failed validation.") String field,
            @Schema(description = "Per-field validation message.") String message) {}
}
