package com.thiefspin.bookingsystem.appointments.validation;

import com.thiefspin.bookingsystem.appointments.AppointmentEntity;
import com.thiefspin.bookingsystem.appointments.AppointmentRepository;
import com.thiefspin.bookingsystem.appointments.AppointmentStatus;
import com.thiefspin.bookingsystem.branches.Branch;
import com.thiefspin.bookingsystem.branches.BranchService;
import com.thiefspin.bookingsystem.util.exceptions.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentValidator Tests")
class AppointmentValidatorTest {

    @Mock
    private AppointmentRepository repository;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private AppointmentValidator validator;

    private Branch testBranch;
    private AppointmentEntity testEntity;

    @BeforeEach
    void setUp() {
        testBranch = new Branch(
            1L,
            "JHB-001",
            "Johannesburg Central",
            "123 Main Street",
            "+27111234567",
            LocalTime.of(8, 0),
            LocalTime.of(17, 0),
            3
        );

        testEntity = new AppointmentEntity(
            1L,
            "BK12345678",
            1L,
            "John",
            "Doe",
            "john.doe@example.com",
            "+27821234567",
            LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
            30,
            "Consultation",
            "Notes",
            AppointmentStatus.CONFIRMED.name(),
            Instant.now(),
            Instant.now(),
            null,
            null
        );
    }

    @Nested
    @DisplayName("Validate Slot Available Tests")
    class ValidateSlotAvailableTests {

        @Test
        @DisplayName("Should pass validation when slot is available")
        void shouldPassWhenSlotAvailable() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
            when(repository.countActiveAppointmentsAtTime(1L, appointmentTime)).thenReturn(2);

            // When & Then
            assertThatNoException().isThrownBy(() ->
                validator.validateSlotAvailable(1L, appointmentTime)
            );

            verify(branchService).findById(1L);
            verify(repository).countActiveAppointmentsAtTime(1L, appointmentTime);
        }

        @Test
        @DisplayName("Should throw exception when slot is fully booked")
        void shouldThrowExceptionWhenSlotFullyBooked() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
            when(repository.countActiveAppointmentsAtTime(1L, appointmentTime)).thenReturn(3);

            // When & Then
            assertThatThrownBy(() -> validator.validateSlotAvailable(1L, appointmentTime))
                .isInstanceOf(BadRequestException.class);

            verify(branchService).findById(1L);
            verify(repository).countActiveAppointmentsAtTime(1L, appointmentTime);
        }

        @Test
        @DisplayName("Should throw exception when branch doesn't exist")
        void shouldThrowExceptionWhenBranchDoesNotExist() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> validator.validateSlotAvailable(999L, appointmentTime))
                .isInstanceOf(BadRequestException.class);

            verify(branchService).findById(999L);
            verify(repository, never()).countActiveAppointmentsAtTime(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Validate Within Operating Hours Tests")
    class ValidateWithinOperatingHoursTests {

        @Test
        @DisplayName("Should pass validation when appointment is within operating hours")
        void shouldPassWhenWithinOperatingHours() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

            // When & Then
            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30)
            );
        }

        @Test
        @DisplayName("Should throw exception when appointment starts before opening time")
        void shouldThrowExceptionWhenStartsBeforeOpening() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(7).withMinute(30);

            // When & Then
            assertThatThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception when appointment ends after closing time")
        void shouldThrowExceptionWhenEndsAfterClosing() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(16).withMinute(45);

            // When & Then
            assertThatThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception when appointment ends after closing time")
        void shouldThrowExceptionWhenAppointmentEndsAfterClosingTime() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(16).withMinute(31);

            // When & Then
            assertThatThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should allow appointment that starts exactly at opening time")
        void shouldAllowAppointmentStartingAtOpeningTime() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);

            // When & Then
            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30)
            );
        }
    }

    @Nested
    @DisplayName("Validate Cancellable Tests")
    class ValidateCancellableTests {

        @Test
        @DisplayName("Should pass validation for CONFIRMED appointment in future")
        void shouldPassForConfirmedFutureAppointment() {
            // Given
            AppointmentEntity futureAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().plusDays(1), 30, "Consultation", null,
                AppointmentStatus.CONFIRMED.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatNoException().isThrownBy(() ->
                validator.validateCancellable(futureAppointment)
            );
        }

        @Test
        @DisplayName("Should pass validation for PENDING appointment in future")
        void shouldPassForPendingFutureAppointment() {
            // Given
            AppointmentEntity futureAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().plusDays(1), 30, "Consultation", null,
                AppointmentStatus.PENDING.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatNoException().isThrownBy(() ->
                validator.validateCancellable(futureAppointment)
            );
        }

        @Test
        @DisplayName("Should throw exception for already CANCELLED appointment")
        void shouldThrowExceptionForCancelledAppointment() {
            // Given
            AppointmentEntity cancelledAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().plusDays(1), 30, "Consultation", null,
                AppointmentStatus.CANCELLED.name(),
                Instant.now(), Instant.now(), Instant.now(), "Customer requested"
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateCancellable(cancelledAppointment))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception for COMPLETED appointment")
        void shouldThrowExceptionForCompletedAppointment() {
            // Given
            AppointmentEntity completedAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().minusDays(1), 30, "Consultation", null,
                AppointmentStatus.COMPLETED.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateCancellable(completedAppointment))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception for NO_SHOW appointment")
        void shouldThrowExceptionForNoShowAppointment() {
            // Given
            AppointmentEntity noShowAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().minusDays(1), 30, "Consultation", null,
                AppointmentStatus.NO_SHOW.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateCancellable(noShowAppointment))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception for past CONFIRMED appointment")
        void shouldThrowExceptionForPastConfirmedAppointment() {
            // Given
            AppointmentEntity pastAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().minusDays(1), 30, "Consultation", null,
                AppointmentStatus.CONFIRMED.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateCancellable(pastAppointment))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw exception for past PENDING appointment")
        void shouldThrowExceptionForPastPendingAppointment() {
            // Given
            AppointmentEntity pastAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().minusDays(1), 30, "Consultation", null,
                AppointmentStatus.PENDING.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatThrownBy(() -> validator.validateCancellable(pastAppointment))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should allow cancellation of appointment starting in next hour")
        void shouldAllowCancellationOfAppointmentStartingSoon() {
            // Given
            AppointmentEntity soonAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().plusMinutes(30), 30, "Consultation", null,
                AppointmentStatus.CONFIRMED.name(),
                Instant.now(), Instant.now(), null, null
            );

            // When & Then
            assertThatNoException().isThrownBy(() ->
                validator.validateCancellable(soonAppointment)
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases And Additional Coverage Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle appointment at exact opening time")
        void shouldHandleAppointmentAtExactOpeningTime() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0).withSecond(0);

            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30)
            );
        }

        @Test
        @DisplayName("Should throw exception when appointment crosses closing boundary by one minute")
        void shouldThrowExceptionWhenCrossingClosingBoundary() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(16).withMinute(31).withSecond(0);

            assertThatThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should allow appointment ending exactly at closing time")
        void shouldAllowAppointmentEndingAtClosingTime() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(16).withMinute(30).withSecond(0).withNano(0);

            // When/Then
            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 30)
            );
        }

        @Test
        @DisplayName("Should validate slot with exactly available capacity")
        void shouldValidateSlotWithExactCapacity() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
            when(repository.countActiveAppointmentsAtTime(1L, appointmentTime)).thenReturn(2);

            assertThatNoException().isThrownBy(() ->
                validator.validateSlotAvailable(1L, appointmentTime)
            );

            verify(branchService).findById(1L);
            verify(repository).countActiveAppointmentsAtTime(1L, appointmentTime);
        }

        @Test
        @DisplayName("Should throw exception when slot is at maximum capacity")
        void shouldThrowExceptionAtMaxCapacity() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
            when(repository.countActiveAppointmentsAtTime(1L, appointmentTime)).thenReturn(3);

            assertThatThrownBy(() -> validator.validateSlotAvailable(1L, appointmentTime))
                .isInstanceOf(BadRequestException.class);

            verify(branchService).findById(1L);
            verify(repository).countActiveAppointmentsAtTime(1L, appointmentTime);
        }

        @Test
        @DisplayName("Should allow cancellation exactly at current time boundary")
        void shouldAllowCancellationAtCurrentTimeBoundary() {
            AppointmentEntity boundaryAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().plusSeconds(1), 30, "Consultation", null,
                AppointmentStatus.CONFIRMED.name(),
                Instant.now(), Instant.now(), null, null
            );

            assertThatNoException().isThrownBy(() ->
                validator.validateCancellable(boundaryAppointment)
            );
        }

        @Test
        @DisplayName("Should throw exception when branch does not exist for slot validation")
        void shouldThrowExceptionWhenBranchNotFoundForSlot() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> validator.validateSlotAvailable(999L, appointmentTime))
                .isInstanceOf(BadRequestException.class);

            verify(branchService).findById(999L);
            verify(repository, never()).countActiveAppointmentsAtTime(anyLong(), any());
        }

        @Test
        @DisplayName("Should handle appointment with very long duration within operating hours")
        void shouldHandleLongDurationWithinHours() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            Branch allDayBranch = new Branch(
                2L, "JHB-002", "All Day Branch", "456 Long St",
                "+27111234568", LocalTime.of(9, 0), LocalTime.of(21, 0), 3
            );

            // When/Then
            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(allDayBranch, appointmentTime, 720)
            );
        }

        @Test
        @DisplayName("Should throw exception for appointment with very long duration exceeding hours")
        void shouldThrowExceptionForLongDurationExceedingHours() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(8).withMinute(0);

            assertThatThrownBy(() ->
                validator.validateWithinOperatingHours(testBranch, appointmentTime, 600))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should validate cancellable for appointment with minimal future time")
        void shouldValidateCancellableWithMinimalFutureTime() {
            AppointmentEntity almostNowAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().plusSeconds(500), 30, "Consultation", null,
                AppointmentStatus.CONFIRMED.name(),
                Instant.now(), Instant.now(), null, null
            );

            assertThatNoException().isThrownBy(() ->
                validator.validateCancellable(almostNowAppointment)
            );
        }

        @Test
        @DisplayName("Should throw exception for PENDING appointment in past")
        void shouldThrowExceptionForPastPendingAppointmentCancellation() {
            AppointmentEntity pastPendingAppointment = new AppointmentEntity(
                1L, "BK12345678", 1L, "John", "Doe", "john@example.com", "+27821234567",
                LocalDateTime.now().minusHours(2), 30, "Consultation", null,
                AppointmentStatus.PENDING.name(),
                Instant.now(), Instant.now(), null, null
            );

            assertThatThrownBy(() -> validator.validateCancellable(pastPendingAppointment))
                .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should validate slot available with zero current bookings")
        void shouldValidateSlotWithZeroBookings() {
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
            when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
            when(repository.countActiveAppointmentsAtTime(1L, appointmentTime)).thenReturn(0);

            assertThatNoException().isThrownBy(() ->
                validator.validateSlotAvailable(1L, appointmentTime)
            );

            verify(repository).countActiveAppointmentsAtTime(1L, appointmentTime);
        }

        @Test
        @DisplayName("Should handle appointment starting at midnight")
        void shouldHandleAppointmentAtMidnight() {
            LocalDateTime midnightTime = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0);
            Branch midnightBranch = new Branch(
                3L, "24HRS-001", "24 Hour Branch", "999 Night St",
                "+27111234569", LocalTime.of(0, 0), LocalTime.of(23, 59), 3
            );

            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(midnightBranch, midnightTime, 30)
            );
        }

        @Test
        @DisplayName("Should validate operating hours with minimal operating window")
        void shouldValidateMinimalOperatingWindow() {
            // Given
            LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
            Branch minimalBranch = new Branch(
                4L, "MINIMAL-001", "Minimal Hours Branch", "111 Quick St",
                "+27111234570", LocalTime.of(12, 0), LocalTime.of(12, 30), 1
            );

            // When/Then
            assertThatNoException().isThrownBy(() ->
                validator.validateWithinOperatingHours(minimalBranch, appointmentTime, 30)
            );
        }
    }
}
