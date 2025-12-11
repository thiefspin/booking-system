package com.thiefspin.bookingsystem.controllers;

import com.thiefspin.bookingsystem.appointments.Appointment;
import com.thiefspin.bookingsystem.appointments.AppointmentService;
import com.thiefspin.bookingsystem.appointments.requests.AppointmentRequest;
import com.thiefspin.bookingsystem.appointments.slots.TimeSlot;
import com.thiefspin.bookingsystem.util.exceptions.ApiErrorResponse;
import com.thiefspin.bookingsystem.util.exceptions.BadRequestException;
import com.thiefspin.bookingsystem.util.exceptions.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

  private final AppointmentService service;

  @GetMapping("/slots")
  @Operation(
      summary = "Get available time slots",
      description = "Returns available appointment slots for a branch on a specific date."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "List of available time slots",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TimeSlot.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Branch not found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      )
  })
  public List<TimeSlot> getAvailableSlots(
      @RequestParam Long branchId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) throws NotFoundException {
    return service.getAvailableSlots(branchId, date);
  }

  @PostMapping("/book")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Book a new appointment",
      description = "Creates a new appointment booking and returns the confirmation with booking reference."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201",
          description = "Appointment booked successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Appointment.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Invalid booking data or slot not available",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Branch not found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      )
  })
  public Appointment create(@Valid @RequestBody AppointmentRequest request)
      throws NotFoundException, BadRequestException {
    return service.createAppointment(request);
  }

  @GetMapping("/lookup")
  @Operation(
      summary = "Look up appointment",
      description = "Returns appointment details using customer email and booking reference."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Appointment found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Appointment.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Appointment not found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      )
  })
  public Appointment lookupAppointment(
      @RequestParam String email,
      @RequestParam String bookingReference) throws NotFoundException {
    return service.findByEmailAndReference(email, bookingReference)
        .orElseThrow(() -> new NotFoundException("Appointment not found"));
  }

  @PutMapping("/cancel")
  @Operation(
      summary = "Cancel an appointment",
      description = "Cancels an appointment using email and booking reference."
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200",
          description = "Appointment cancelled successfully",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Appointment.class)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "Appointment not found",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "Cannot cancel appointment (already cancelled or completed)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiErrorResponse.class)
          )
      )
  })
  public Appointment cancelAppointment(
      @RequestParam String email,
      @RequestParam String bookingReference,
      @RequestParam(required = false) String reason) throws NotFoundException, BadRequestException {
    service.findByEmailAndReference(email, bookingReference)
        .orElseThrow(() -> new NotFoundException("Appointment not found"));
    return service.cancelAppointment(bookingReference, reason != null ? reason : "Customer requested cancellation");
  }
}
