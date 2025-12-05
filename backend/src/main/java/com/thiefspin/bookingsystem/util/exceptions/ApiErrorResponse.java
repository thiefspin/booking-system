package com.thiefspin.bookingsystem.util.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(description = "Standard error response for API exceptions")
public record ApiErrorResponse(

    @Schema(description = "HTTP status code",
        example = "404",
        minimum = "100",
        maximum = "599",
        required = true)
    @NotNull Integer status,

    @Schema(description = "Human-readable error message",
        example = "Appointment not found with the provided booking reference",
        required = true)
    @NotNull String message,

    @Schema(description = "Timestamp when the error occurred",
        example = "2024-12-20T10:30:00Z",
        format = "date-time",
        accessMode = Schema.AccessMode.READ_ONLY)
    Instant timestamp
) {}
