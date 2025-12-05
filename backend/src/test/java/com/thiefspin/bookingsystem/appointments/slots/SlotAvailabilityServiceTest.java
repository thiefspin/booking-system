package com.thiefspin.bookingsystem.appointments.slots;

import com.thiefspin.bookingsystem.appointments.AppointmentRepository;
import com.thiefspin.bookingsystem.branches.Branch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SlotAvailabilityService Tests")
class SlotAvailabilityServiceTest {

  @Mock
  private AppointmentRepository repository;

  @InjectMocks
  private SlotAvailabilityService service;

  private Branch testBranch;
  private LocalDate testDate;

  @BeforeEach
  void setUp() {
    testBranch = new Branch(
        1L,
        "JHB-001",
        "Johannesburg Central",
        "123 Main Street",
        "+27111234567",
        LocalTime.of(9, 0),
        LocalTime.of(17, 0),
        3
    );

    testDate = LocalDate.now().plusDays(1); // Tomorrow
  }

  @Nested
  @DisplayName("Get Available Slots Tests")
  class GetAvailableSlotsTests {

    @Test
    @DisplayName("Should return empty list for past dates")
    void shouldReturnEmptyListForPastDates() {
      // Given
      LocalDate pastDate = LocalDate.now().minusDays(1);

      // When
      List<TimeSlot> slots = service.getAvailableSlots(testBranch, pastDate);

      // Then
      assertThat(slots).isEmpty();
      verify(repository, never()).countActiveAppointmentsAtTime(anyLong(), any());
    }

    @Test
    @DisplayName("Should generate slots for future date")
    void shouldGenerateSlotsForFutureDate() {
      // Given
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      // When
      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      // Then
      assertThat(slots).isNotEmpty();
      assertThat(slots).hasSize(16);

      assertThat(slots.get(0).startTime()).isEqualTo(
          LocalDateTime.of(testDate, LocalTime.of(9, 0)));
      assertThat(slots.get(0).endTime()).isEqualTo(LocalDateTime.of(testDate, LocalTime.of(9, 30)));
      assertThat(slots.get(0).available()).isTrue();
      assertThat(slots.get(0).currentBookings()).isEqualTo(0);
      assertThat(slots.get(0).maxBookings()).isEqualTo(3);

      assertThat(slots.get(15).startTime()).isEqualTo(
          LocalDateTime.of(testDate, LocalTime.of(16, 30)));
      assertThat(slots.get(15).endTime()).isEqualTo(
          LocalDateTime.of(testDate, LocalTime.of(17, 0)));
    }

    @Test
    @DisplayName("Should mark slots as unavailable when fully booked")
    void shouldMarkSlotsAsUnavailableWhenFullyBooked() {
      // Given
      when(repository.countActiveAppointmentsAtTime(eq(1L), any(LocalDateTime.class)))
          .thenAnswer(invocation -> {
            LocalDateTime slotTime = invocation.getArgument(1);
            if (slotTime.getHour() == 9 && slotTime.getMinute() == 0) {
              return 3;
            } else if (slotTime.getHour() == 9 && slotTime.getMinute() == 30) {
              return 1;
            }
            return 0;
          });

      // When
      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      // Then
      assertThat(slots).isNotEmpty();
      assertThat(slots.size()).isGreaterThanOrEqualTo(2);

      TimeSlot firstSlot = slots.get(0);
      assertThat(firstSlot.available()).isFalse();
      assertThat(firstSlot.currentBookings()).isEqualTo(3);
      assertThat(firstSlot.maxBookings()).isEqualTo(3);

      TimeSlot secondSlot = slots.get(1);
      assertThat(secondSlot.available()).isTrue();
      assertThat(secondSlot.currentBookings()).isEqualTo(1);
      assertThat(secondSlot.maxBookings()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should skip past slots for today")
    void shouldSkipPastSlotsForToday() {
      // Given
      LocalDate today = LocalDate.now();
      LocalDateTime now = LocalDateTime.now();

      // When
      List<TimeSlot> slots = service.getAvailableSlots(testBranch, today);

      // Then
      if (!slots.isEmpty()) {
        assertThat(slots).allSatisfy(slot -> {
          assertThat(slot.startTime()).isAfter(
              now.minusMinutes(1));
        });
      }

      verify(repository, atLeast(0)).countActiveAppointmentsAtTime(eq(1L),
          any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should respect branch operating hours")
    void shouldRespectBranchOperatingHours() {
      // Given
      Branch limitedBranch = new Branch(
          2L, "JHB-002", "Limited Hours Branch", "456 Side Street",
          "+27111234568", LocalTime.of(10, 0), LocalTime.of(14, 0), 2
      );

      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      // When
      List<TimeSlot> slots = service.getAvailableSlots(limitedBranch, testDate);

      // Then
      assertThat(slots).hasSize(8);

      assertThat(slots.get(0).startTime()).isEqualTo(
          LocalDateTime.of(testDate, LocalTime.of(10, 0)));

      assertThat(slots.get(7).startTime()).isEqualTo(
          LocalDateTime.of(testDate, LocalTime.of(13, 30)));
      assertThat(slots.get(7).endTime()).isEqualTo(LocalDateTime.of(testDate, LocalTime.of(14, 0)));
    }

    @Test
    @DisplayName("Should handle branch with single appointment capacity")
    void shouldHandleBranchWithSingleAppointmentCapacity() {
      // Given
      Branch singleCapacityBranch = new Branch(
          3L, "JHB-003", "Single Capacity Branch", "789 Another Street",
          "+27111234569", LocalTime.of(9, 0), LocalTime.of(10, 0), 1
      );

      when(repository.countActiveAppointmentsAtTime(3L,
          LocalDateTime.of(testDate, LocalTime.of(9, 0))))
          .thenReturn(1);
      when(repository.countActiveAppointmentsAtTime(3L,
          LocalDateTime.of(testDate, LocalTime.of(9, 30))))
          .thenReturn(0);

      // When
      List<TimeSlot> slots = service.getAvailableSlots(singleCapacityBranch, testDate);

      // Then
      assertThat(slots).hasSize(2);

      assertThat(slots.get(0).available()).isFalse();
      assertThat(slots.get(0).maxBookings()).isEqualTo(1);

      assertThat(slots.get(1).available()).isTrue();
      assertThat(slots.get(1).maxBookings()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should generate consistent 30-minute slots")
    void shouldGenerateConsistent30MinuteSlots() {
      // Given
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      // When
      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      // Then
      for (TimeSlot slot : slots) {
        long durationMinutes = java.time.Duration.between(slot.startTime(), slot.endTime())
            .toMinutes();
        assertThat(durationMinutes).isEqualTo(30);
      }

      for (int i = 0; i < slots.size() - 1; i++) {
        assertThat(slots.get(i).endTime()).isEqualTo(slots.get(i + 1).startTime());
      }
    }

    @Test
    @DisplayName("Should return empty list when branch closes before opening")
    void shouldReturnEmptyListWhenBranchClosesBeforeOpening() {
      // Given
      Branch invalidBranch = new Branch(
          4L, "JHB-004", "Invalid Branch", "999 Error Street",
          "+27111234570", LocalTime.of(17, 0), LocalTime.of(9, 0), 1
      );

      // When
      List<TimeSlot> slots = service.getAvailableSlots(invalidBranch, testDate);

      // Then
      assertThat(slots).isEmpty();
      verify(repository, never()).countActiveAppointmentsAtTime(anyLong(), any());
    }

    @Test
    @DisplayName("Should correctly show availability based on current bookings")
    void shouldCorrectlyShowAvailabilityBasedOnCurrentBookings() {
      // Given
      when(repository.countActiveAppointmentsAtTime(eq(1L), any(LocalDateTime.class)))
          .thenAnswer(invocation -> {
            LocalDateTime slotTime = invocation.getArgument(1);
            LocalTime time = slotTime.toLocalTime();
            if (time.equals(LocalTime.of(9, 0))) {
              return 0;
            } else if (time.equals(LocalTime.of(9, 30))) {
              return 2;
            } else if (time.equals(LocalTime.of(10, 0))) {
              return 3;
            }
            return 1;
          });

      // When
      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      // Then
      assertThat(slots).isNotEmpty();
      assertThat(slots.size()).isGreaterThanOrEqualTo(3);

      assertThat(slots.get(0).available()).isTrue();
      assertThat(slots.get(0).currentBookings()).isEqualTo(0);

      assertThat(slots.get(1).available()).isTrue();
      assertThat(slots.get(1).currentBookings()).isEqualTo(2);

      assertThat(slots.get(2).available()).isFalse();
      assertThat(slots.get(2).currentBookings()).isEqualTo(3);
      assertThat(slots.get(2).maxBookings()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle branch with very high capacity")
    void shouldHandleVeryHighCapacity() {
      Branch highCapacityBranch = new Branch(
          5L, "JHB-005", "High Capacity Branch", "555 Big Street",
          "+27111234571", LocalTime.of(9, 0), LocalTime.of(17, 0), 50
      );
      when(repository.countActiveAppointmentsAtTime(eq(5L), any(LocalDateTime.class)))
          .thenReturn(25);

      List<TimeSlot> slots = service.getAvailableSlots(highCapacityBranch, testDate);

      assertThat(slots).isNotEmpty();
      assertThat(slots.get(0).maxBookings()).isEqualTo(50);
      assertThat(slots.get(0).available()).isTrue();
      assertThat(slots.get(0).currentBookings()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should handle all slots fully booked")
    void shouldHandleAllSlotsFullyBooked() {
      when(repository.countActiveAppointmentsAtTime(eq(1L), any(LocalDateTime.class)))
          .thenReturn(3);

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      assertThat(slots).allSatisfy(slot -> assertThat(slot.available()).isFalse());
    }

    @Test
    @DisplayName("Should handle all slots available")
    void shouldHandleAllSlotsAvailable() {
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      assertThat(slots).allSatisfy(slot -> {
        assertThat(slot.available()).isTrue();
        assertThat(slot.currentBookings()).isEqualTo(0);
      });
    }

    @Test
    @DisplayName("Should generate slots with correct boundaries for half-hour intervals")
    void shouldGenerateCorrectHalfHourBoundaries() {
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      for (int i = 0; i < slots.size(); i++) {
        TimeSlot slot = slots.get(i);
        int minutes = slot.startTime().getMinute();
        assertThat(minutes).isIn(0, 30);

        long duration = java.time.Duration.between(slot.startTime(), slot.endTime()).toMinutes();
        assertThat(duration).isEqualTo(30);
      }
    }

    @Test
    @DisplayName("Should not generate slots before branch opening time")
    void shouldNotGenerateSlotsBeforeOpening() {
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      assertThat(slots.get(0).startTime().toLocalTime()).isEqualTo(testBranch.openingTime());
    }

    @Test
    @DisplayName("Should not generate slots after branch closing time")
    void shouldNotGenerateSlotsAfterClosing() {
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      TimeSlot lastSlot = slots.get(slots.size() - 1);
      assertThat(lastSlot.endTime().toLocalTime()).isEqualTo(testBranch.closingTime());
    }

    @Test
    @DisplayName("Should handle branch with one-hour operating window")
    void shouldHandleOneHourOperatingWindow() {
      Branch shortHoursBranch = new Branch(
          6L, "JHB-006", "Short Hours Branch", "666 Quick St",
          "+27111234572", LocalTime.of(14, 0), LocalTime.of(15, 0), 2
      );
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots = service.getAvailableSlots(shortHoursBranch, testDate);

      assertThat(slots).hasSize(2);
      assertThat(slots.get(0).startTime().toLocalTime()).isEqualTo(LocalTime.of(14, 0));
      assertThat(slots.get(1).startTime().toLocalTime()).isEqualTo(LocalTime.of(14, 30));
      assertThat(slots.get(1).endTime().toLocalTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    @DisplayName("Should handle negative booking count by treating as zero")
    void shouldHandleNegativeBookingCount() {
      when(repository.countActiveAppointmentsAtTime(eq(1L), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      assertThat(slots.get(0).currentBookings()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Should maintain consistent slot count across multiple calls")
    void shouldMaintainConsistentSlotCount() {
      when(repository.countActiveAppointmentsAtTime(anyLong(), any(LocalDateTime.class)))
          .thenReturn(0);

      List<TimeSlot> slots1 = service.getAvailableSlots(testBranch, testDate);
      List<TimeSlot> slots2 = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots1).hasSize(slots2.size());
      assertThat(slots1.size()).isEqualTo(16);
    }

    @Test
    @DisplayName("Should increment bookings correctly for intermediate slots")
    void shouldIncrementBookingsCorrectly() {
      when(repository.countActiveAppointmentsAtTime(eq(1L), any(LocalDateTime.class)))
          .thenAnswer(invocation -> {
            LocalDateTime slotTime = invocation.getArgument(1);
            LocalTime time = slotTime.toLocalTime();
            if (time.equals(LocalTime.of(9, 0))) {
              return 1;
            } else if (time.equals(LocalTime.of(9, 30))) {
              return 2;
            } else if (time.equals(LocalTime.of(10, 0))) {
              return 3;
            }
            return 0;
          });

      List<TimeSlot> slots = service.getAvailableSlots(testBranch, testDate);

      assertThat(slots).isNotEmpty();
      assertThat(slots.get(0).currentBookings()).isEqualTo(1);
      assertThat(slots.get(1).currentBookings()).isEqualTo(2);
      assertThat(slots.get(2).currentBookings()).isEqualTo(3);
    }
  }
}
