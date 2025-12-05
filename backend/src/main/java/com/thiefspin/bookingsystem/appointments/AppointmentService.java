package com.thiefspin.bookingsystem.appointments;

import com.thiefspin.bookingsystem.BookingReferenceGenerator;
import com.thiefspin.bookingsystem.appointments.requests.AppointmentRequest;
import com.thiefspin.bookingsystem.appointments.slots.SlotAvailabilityService;
import com.thiefspin.bookingsystem.appointments.slots.TimeSlot;
import com.thiefspin.bookingsystem.appointments.validation.AppointmentValidator;
import com.thiefspin.bookingsystem.branches.Branch;
import com.thiefspin.bookingsystem.branches.BranchService;
import com.thiefspin.bookingsystem.notifications.NotificationService;
import com.thiefspin.bookingsystem.util.exceptions.BadRequestException;
import com.thiefspin.bookingsystem.util.exceptions.NotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AppointmentService {

  private AppointmentRepository repository;

  private BranchService branchService;

  private BookingReferenceGenerator referenceGenerator;

  private AppointmentValidator validator;

  private SlotAvailabilityService slotAvailabilityService;

  private NotificationService notificationService;

  public Optional<Appointment> findByEmailAndReference(String email, String bookingReference) {
    return repository.findByCustomerEmailAndBookingReference(email, bookingReference)
        .stream()
        .findFirst()
        .map(AppointmentEntity::toModel);
  }

  @Transactional
  public Appointment createAppointment(AppointmentRequest request)
      throws NotFoundException, BadRequestException {
    var branch = fetchBranch(request.branchId());

    validator.validateSlotAvailable(request.branchId(), request.appointmentDateTime());
    validator.validateWithinOperatingHours(branch, request.appointmentDateTime(),
        request.durationMinutes());

    String bookingReference = referenceGenerator.generate();
    AppointmentEntity entity = AppointmentEntity.fromRequest(request, bookingReference);
    AppointmentEntity saved = repository.save(entity);

    notificationService.sendConfirmationEvent(saved);

    return saved.toModel();
  }

  @Transactional
  public Appointment cancelAppointment(String bookingReference, String reason)
      throws NotFoundException, BadRequestException {
    AppointmentEntity appointment = repository.findByBookingReference(bookingReference)
        .orElseThrow(() -> new NotFoundException("Appointment not found"));

    validator.validateCancellable(appointment);

    AppointmentEntity cancelled = appointment.withCancellation(reason, Instant.now());
    AppointmentEntity saved = repository.save(cancelled);

    notificationService.sendCancellationEvent(saved);

    return saved.toModel();
  }

  public List<TimeSlot> getAvailableSlots(Long branchId, LocalDate date) throws NotFoundException {
    var branch = fetchBranch(branchId);
    return slotAvailabilityService.getAvailableSlots(branch, date);
  }

  private Branch fetchBranch(Long branchId) throws NotFoundException {
    return branchService.findById(branchId)
        .orElseThrow(() -> new NotFoundException("Branch not found"));
  }
}
