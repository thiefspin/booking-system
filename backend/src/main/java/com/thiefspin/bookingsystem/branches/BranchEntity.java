package com.thiefspin.bookingsystem.branches;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalTime;

@Table(schema = "booking", name = "branches")
public record BranchEntity(

    @Id
    Long id,

    @Column("code")
    String code,

    @Column("name")
    String name,

    @Column("address")
    String address,

    @Column("phone_number")
    String phoneNumber,

    @Column("email")
    String email,

    @Column("opening_time")
    LocalTime openingTime,

    @Column("closing_time")
    LocalTime closingTime,

    @Column("max_concurrent_appointments_per_slot")
    Integer maxConcurrentAppointmentsPerSlot,

    @Column("is_active")
    Boolean isActive,

    @Column("created_at")
    Instant createdAt,

    @Column("updated_at")
    Instant updatedAt
) {

  public Branch toModel() {
    return new Branch(
        id,
        code,
        name,
        address,
        phoneNumber,
        openingTime,
        closingTime,
        maxConcurrentAppointmentsPerSlot
    );
  }
}
