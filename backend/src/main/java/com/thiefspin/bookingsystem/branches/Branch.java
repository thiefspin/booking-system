package com.thiefspin.bookingsystem.branches;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "Branch location details and operating hours")
public record Branch(

    @Schema(description = "Unique identifier of the branch",
        example = "1",
        accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @Schema(description = "Unique branch code",
        example = "JHB-001",
        pattern = "^[A-Z]{3}-[0-9]{3}$")
    String code,

    @Schema(description = "Full name of the branch",
        example = "Johannesburg Central Branch",
        maxLength = 100)
    String name,

    @Schema(description = "Physical address of the branch",
        example = "123 Main Street, Johannesburg, 2001",
        maxLength = 255)
    String address,

    @Schema(description = "Contact phone number for the branch",
        example = "+27 11 123 4567",
        pattern = "^[\\+]?[0-9\\-\\s\\(\\)]+$")
    String phoneNumber,

    @Schema(description = "Branch opening time",
        example = "08:00:00",
        format = "time",
        pattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")
    LocalTime openingTime,

    @Schema(description = "Branch closing time",
        example = "17:00:00",
        format = "time",
        pattern = "^([01]?[0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$")
    LocalTime closingTime,

    @Schema(description = "Maximum number of appointments that can be booked in the same time slot",
        example = "3",
        minimum = "1",
        maximum = "10",
        defaultValue = "1")
    Integer maxConcurrentAppointmentsPerSlot
) {}
