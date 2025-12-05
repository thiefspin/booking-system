package com.thiefspin.bookingsystem.appointments.slots;

import com.thiefspin.bookingsystem.appointments.AppointmentRepository;
import com.thiefspin.bookingsystem.branches.Branch;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class SlotAvailabilityService {

  private AppointmentRepository repository;

  private static final int DEFAULT_SLOT_DURATION_MINUTES = 30;

  public List<TimeSlot> getAvailableSlots(Branch branch, LocalDate date) {
    if (date.isBefore(LocalDate.now())) {
      log.debug("Date is in the past, returning empty slots");
      return Collections.emptyList();
    }

    return generateSlots(branch, date);
  }

  private List<TimeSlot> generateSlots(Branch branch, LocalDate date) {
    List<TimeSlot> slots = new ArrayList<>();
    LocalTime currentTime = branch.openingTime();
    LocalTime closingTime = branch.closingTime();

    while (canCreateSlot(currentTime, closingTime)) {
      LocalDateTime slotDateTime = LocalDateTime.of(date, currentTime);

      if (slotDateTime.isBefore(LocalDateTime.now())) {
        currentTime = currentTime.plusMinutes(DEFAULT_SLOT_DURATION_MINUTES);
        continue;
      }

      TimeSlot slot = createTimeSlot(branch, slotDateTime);
      slots.add(slot);

      currentTime = currentTime.plusMinutes(DEFAULT_SLOT_DURATION_MINUTES);
    }
    return slots;
  }

  private boolean canCreateSlot(LocalTime currentTime, LocalTime closingTime) {
    LocalTime slotEndTime = currentTime.plusMinutes(DEFAULT_SLOT_DURATION_MINUTES);
    return slotEndTime.isBefore(closingTime) || slotEndTime.equals(closingTime);
  }

  private TimeSlot createTimeSlot(Branch branch, LocalDateTime slotDateTime) {
    LocalDateTime slotEndDateTime = slotDateTime.plusMinutes(DEFAULT_SLOT_DURATION_MINUTES);
    Integer currentBookings = repository.countActiveAppointmentsAtTime(branch.id(), slotDateTime);
    Integer maxBookings = branch.maxConcurrentAppointmentsPerSlot();

    return new TimeSlot(
        slotDateTime,
        slotEndDateTime,
        currentBookings < maxBookings,
        currentBookings,
        maxBookings
    );
  }
}
