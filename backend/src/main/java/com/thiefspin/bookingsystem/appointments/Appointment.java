package com.thiefspin.bookingsystem.appointments;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Appointment details including customer information and booking status")
public record Appointment(

    @Schema(description = "Unique identifier of the appointment",
        example = "123",
        accessMode = Schema.AccessMode.READ_ONLY)
    Long id,

    @Schema(description = "Unique booking reference code for the appointment",
        example = "BK12345678",
        pattern = "^BK[A-Z0-9]{8}$",
        accessMode = Schema.AccessMode.READ_ONLY)
    @NotBlank(message = "Booking reference is required")
    String bookingReference,

    @Schema(description = "ID of the branch where the appointment is scheduled",
        example = "1",
        required = true)
    @NotNull(message = "Branch is required")
    Long branchId,

    @Schema(description = "Customer's first name",
        example = "John",
        minLength = 1,
        maxLength = 100)
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    String customerFirstName,

    @Schema(description = "Customer's last name",
        example = "Doe",
        minLength = 1,
        maxLength = 100)
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    String customerLastName,

    @Schema(description = "Customer's email address",
        example = "john.doe@example.com",
        format = "email")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String customerEmail,

    @Schema(description = "Customer's contact phone number",
        example = "+27 82 123 4567",
        pattern = "^[\\+]?[0-9\\-\\s\\(\\)]+$")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[\\+]?[0-9\\-\\s\\(\\)]+$", message = "Phone number format is invalid")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    String customerPhone,

    // Appointment details
    @Schema(description = "Scheduled date and time of the appointment",
        example = "2024-12-25T10:30:00",
        format = "date-time")
    @NotNull(message = "Appointment date and time is required")
    @Future(message = "Appointment must be in the future")
    LocalDateTime appointmentDateTime,

    @Schema(description = "Duration of the appointment in minutes",
        example = "30",
        minimum = "15")
    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Appointment duration must be at least 15 minutes")
    Integer durationMinutes,

    @Schema(description = "Purpose of the appointment",
        example = "Annual health check-up",
        maxLength = 500,
        nullable = true)
    @Size(max = 500, message = "Purpose must not exceed 500 characters")
    String purpose,

    @Schema(description = "Additional notes for the appointment",
        example = "Please call me 5 minutes before",
        maxLength = 1000,
        nullable = true)
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    String notes,

    @Schema(description = "Current status of the appointment",
        example = "CONFIRMED",
        implementation = AppointmentStatus.class,
        accessMode = Schema.AccessMode.READ_ONLY)
    AppointmentStatus status
) {}
