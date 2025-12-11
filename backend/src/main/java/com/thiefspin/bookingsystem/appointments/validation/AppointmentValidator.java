package com.thiefspin.bookingsystem.appointments.validation;

import com.thiefspin.bookingsystem.appointments.AppointmentEntity;
import com.thiefspin.bookingsystem.appointments.AppointmentRepository;
import com.thiefspin.bookingsystem.appointments.AppointmentStatus;
import com.thiefspin.bookingsystem.branches.Branch;
import com.thiefspin.bookingsystem.branches.BranchService;
import com.thiefspin.bookingsystem.util.exceptions.BadRequestException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AppointmentValidator {

  private final AppointmentRepository repository;

  private final BranchService branchService;

  public void validateSlotAvailable(Long branchId, LocalDateTime dateTime)
      throws BadRequestException {
    if (!isSlotAvailable(branchId, dateTime)) {
      throw new BadRequestException(
          "Slot at %s for branch %d is not available"
              .formatted(dateTime, branchId)
      );
    }
  }

  public void validateWithinOperatingHours(Branch branch, LocalDateTime appointmentDateTime,
      int durationMinutes)
      throws BadRequestException {
    LocalTime appointmentTime = appointmentDateTime.toLocalTime();
    LocalTime endTime = appointmentTime.plusMinutes(durationMinutes);

    if (appointmentTime.isBefore(branch.openingTime()) || endTime.isAfter(branch.closingTime())) {
      throw new BadRequestException("Appointment time is outside branch operating hours");
    }
  }

  public void validateCancellable(AppointmentEntity appointment) throws BadRequestException {
    var status = AppointmentStatus.valueOf(appointment.status());

    switch (status) {
      case CANCELLED -> throw new BadRequestException("Appointment is already cancelled");
      case COMPLETED -> throw new BadRequestException("Cannot cancel a completed appointment");
      case NO_SHOW -> throw new BadRequestException("Cannot cancel a no-show appointment");
      case PENDING, CONFIRMED -> validateNotPastAppointment(appointment);
    }
  }

  private void validateNotPastAppointment(AppointmentEntity appointment)
      throws BadRequestException {
    if (appointment.appointmentDateTime().isBefore(LocalDateTime.now())) {
      throw new BadRequestException("Cannot cancel past appointments");
    }
  }

  private boolean isSlotAvailable(Long branchId, LocalDateTime dateTime) {
    return branchService.findById(branchId)
        .map(branch -> checkCapacity(branchId, dateTime, branch))
        .orElse(false);
  }

  private boolean checkCapacity(Long branchId, LocalDateTime dateTime, Branch branch) {
    return repository.countActiveAppointmentsAtTime(
        branchId, dateTime)
        < branch.maxConcurrentAppointmentsPerSlot();
  }
}
