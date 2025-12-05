package com.thiefspin.bookingsystem.appointments;

import com.thiefspin.bookingsystem.appointments.requests.AppointmentRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(schema = "booking", name = "appointments")
public record AppointmentEntity(

    @Id
    Long id,

    @Column("booking_reference")
    String bookingReference,

    @Column("branch_id")
    Long branchId,

    @Column("customer_first_name")
    String customerFirstName,

    @Column("customer_last_name")
    String customerLastName,

    @Column("customer_email")
    String customerEmail,

    @Column("customer_phone")
    String customerPhone,

    @Column("appointment_date_time")
    LocalDateTime appointmentDateTime,

    @Column("duration_minutes")
    Integer durationMinutes,

    @Column("purpose")
    String purpose,

    @Column("notes")
    String notes,

    @Column("status")
    String status,

    @Column("created_at")
    Instant createdAt,

    @Column("updated_at")
    Instant updatedAt,

    @Column("cancelled_at")
    Instant cancelledAt,

    @Column("cancellation_reason")
    String cancellationReason
) {

  public Appointment toModel() {
    return new Appointment(
        id,
        bookingReference,
        branchId,
        customerFirstName,
        customerLastName,
        customerEmail,
        customerPhone,
        appointmentDateTime,
        durationMinutes,
        purpose,
        notes,
        AppointmentStatus.valueOf(status)
    );
  }

  public static AppointmentEntity fromRequest(AppointmentRequest request, String bookingReference) {
    return new AppointmentEntity(
        null,
        bookingReference,
        request.branchId(),
        request.firstName(),
        request.lastName(),
        request.email(),
        request.phoneNumber(),
        request.appointmentDateTime(),
        request.durationMinutes(),
        request.purpose(),
        request.notes(),
        AppointmentStatus.CONFIRMED.name(),
        Instant.now(),
        Instant.now(),
        null,
        null
    );
  }

  public AppointmentEntity withCancellation(String reason, Instant cancelledAt) {
    return new AppointmentEntity(
        this.id(),
        this.bookingReference(),
        this.branchId(),
        this.customerFirstName(),
        this.customerLastName(),
        this.customerEmail(),
        this.customerPhone(),
        this.appointmentDateTime(),
        this.durationMinutes(),
        this.purpose(),
        this.notes(),
        AppointmentStatus.CANCELLED.name(),
        this.createdAt(),
        cancelledAt,
        cancelledAt,
        reason
    );
  }
}
