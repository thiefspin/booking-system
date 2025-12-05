package com.thiefspin.bookingsystem.appointments;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Status of an appointment throughout its lifecycle")
public enum AppointmentStatus {

  @Schema(description = "Appointment is pending confirmation")
  PENDING,

  @Schema(description = "Appointment has been confirmed and is scheduled")
  CONFIRMED,

  @Schema(description = "Appointment has been cancelled by customer or system")
  CANCELLED,

  @Schema(description = "Appointment has been completed successfully")
  COMPLETED,

  @Schema(description = "Customer did not show up for the appointment")
  NO_SHOW;

  @Schema(description = "Get human-readable description of the status")
  public String getDescription() {
    return switch (this) {
      case PENDING -> "Appointment is awaiting confirmation";
      case CONFIRMED -> "Appointment is confirmed and scheduled";
      case CANCELLED -> "Appointment has been cancelled";
      case COMPLETED -> "Appointment has been completed";
      case NO_SHOW -> "Customer did not attend the appointment";
    };
  }
}
