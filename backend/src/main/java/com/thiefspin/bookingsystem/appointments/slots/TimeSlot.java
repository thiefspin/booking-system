package com.thiefspin.bookingsystem.appointments.slots;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Available time slot for booking appointments")
public record TimeSlot(

    @Schema(description = "Start time of the available slot",
        example = "2024-12-25T10:00:00",
        format = "date-time",
        required = true)
    LocalDateTime startTime,

    @Schema(description = "End time of the available slot",
        example = "2024-12-25T10:30:00",
        format = "date-time",
        required = true)
    LocalDateTime endTime,

    @Schema(description = "Whether this slot is available for booking",
        example = "true",
        required = true)
    boolean available,

    @Schema(description = "Number of appointments already booked in this slot",
        example = "2",
        minimum = "0")
    Integer currentBookings,

    @Schema(description = "Maximum number of concurrent appointments allowed in this slot",
        example = "5",
        minimum = "1")
    Integer maxBookings
) {}
