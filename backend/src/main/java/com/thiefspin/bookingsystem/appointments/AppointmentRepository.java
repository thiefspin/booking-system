package com.thiefspin.bookingsystem.appointments;

import com.thiefspin.bookingsystem.util.repository.BaseDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AppointmentRepository extends BaseDataRepository<AppointmentEntity, Long> {

  Optional<AppointmentEntity> findByBookingReference(String bookingReference);

  @Query("SELECT * FROM booking.appointments " +
      "WHERE branch_id = :branchId " +
      "AND appointment_date_time >= :startDateTime " +
      "AND appointment_date_time < :endDateTime " +
      "AND status IN ('PENDING', 'CONFIRMED')")
  List<AppointmentEntity> findActiveAppointmentsByBranchAndDateRange(
      @Param("branchId") Long branchId,
      @Param("startDateTime") LocalDateTime startDateTime,
      @Param("endDateTime") LocalDateTime endDateTime
  );

  @Query("SELECT COUNT(*) FROM booking.appointments " +
      "WHERE branch_id = :branchId " +
      "AND appointment_date_time = :dateTime " +
      "AND status IN ('PENDING', 'CONFIRMED')")
  Integer countActiveAppointmentsAtTime(
      @Param("branchId") Long branchId,
      @Param("dateTime") LocalDateTime dateTime
  );

  List<AppointmentEntity> findByCustomerEmailAndBookingReference(
      String customerEmail,
      String bookingReference
  );
}
