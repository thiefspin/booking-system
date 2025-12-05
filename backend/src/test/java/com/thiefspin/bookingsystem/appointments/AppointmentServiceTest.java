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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AppointmentService Tests")
class AppointmentServiceTest {

  @Mock
  private AppointmentRepository repository;

  @Mock
  private BranchService branchService;

  @Mock
  private BookingReferenceGenerator referenceGenerator;

  @Mock
  private AppointmentValidator validator;

  @Mock
  private SlotAvailabilityService slotAvailabilityService;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private AppointmentService service;

  private AppointmentRequest validRequest;
  private Branch testBranch;
  private AppointmentEntity testEntity;
  private final String TEST_BOOKING_REF = "BK12345678";

  @BeforeEach
  void setUp() {
    validRequest = new AppointmentRequest(
        1L,
        "John",
        "Doe",
        "john.doe@example.com",
        "+27821234567",
        LocalDateTime.now().plusDays(1),
        30,
        "Consultation",
        "Please call me 5 minutes before"
    );

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
        TEST_BOOKING_REF,
        1L,
        "John",
        "Doe",
        "john.doe@example.com",
        "+27821234567",
        LocalDateTime.now().plusDays(1),
        30,
        "Consultation",
        "Please call me 5 minutes before",
        AppointmentStatus.CONFIRMED.name(),
        Instant.now(),
        Instant.now(),
        null,
        null
    );
  }

  @Nested
  @DisplayName("Create Appointment Tests")
  class CreateAppointmentTests {

    @Test
    @DisplayName("Should successfully create appointment with valid request")
    void shouldCreateAppointmentSuccessfully() throws NotFoundException, BadRequestException {
      // Given
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      // When
      Appointment result = service.createAppointment(validRequest);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.bookingReference()).isEqualTo(TEST_BOOKING_REF);
      assertThat(result.customerFirstName()).isEqualTo("John");
      assertThat(result.customerLastName()).isEqualTo("Doe");
      assertThat(result.status()).isEqualTo(AppointmentStatus.CONFIRMED);

      verify(branchService).findById(1L);
      verify(validator).validateSlotAvailable(1L, validRequest.appointmentDateTime());
      verify(validator).validateWithinOperatingHours(testBranch, validRequest.appointmentDateTime(),
          30);
      verify(referenceGenerator).generate();
      verify(repository).save(any(AppointmentEntity.class));
      verify(notificationService).sendConfirmationEvent(testEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when branch does not exist")
    void shouldThrowNotFoundWhenBranchDoesNotExist() {
      // Given
      when(branchService.findById(1L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> service.createAppointment(validRequest))
          .isInstanceOf(NotFoundException.class);

      verify(repository, never()).save(any());
      verify(notificationService, never()).sendConfirmationEvent(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when slot is not available")
    void shouldThrowBadRequestWhenSlotNotAvailable() throws BadRequestException {
      // Given
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      doThrow(new BadRequestException("Slot not available"))
          .when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));

      // When/Then
      assertThatThrownBy(() -> service.createAppointment(validRequest))
          .isInstanceOf(BadRequestException.class);

      verify(repository, never()).save(any());
      verify(notificationService, never()).sendConfirmationEvent(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when appointment is outside operating hours")
    void shouldThrowBadRequestWhenOutsideOperatingHours() throws BadRequestException {
      // Given
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doThrow(new BadRequestException("Appointment time is outside branch operating hours"))
          .when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      // When/Then
      assertThatThrownBy(() -> service.createAppointment(validRequest))
          .isInstanceOf(BadRequestException.class);

      verify(repository, never()).save(any());
      verify(notificationService, never()).sendConfirmationEvent(any());
    }
  }

  @Nested
  @DisplayName("Cancel Appointment Tests")
  class CancelAppointmentTests {

    @Test
    @DisplayName("Should successfully cancel appointment")
    void shouldCancelAppointmentSuccessfully() throws NotFoundException, BadRequestException {
      // Given
      String cancellationReason = "Unable to attend";
      AppointmentEntity cancelledEntity = testEntity.withCancellation(cancellationReason,
          Instant.now());

      when(repository.findByBookingReference(TEST_BOOKING_REF)).thenReturn(Optional.of(testEntity));
      when(repository.save(any(AppointmentEntity.class))).thenReturn(cancelledEntity);
      when(notificationService.sendCancellationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateCancellable(any(AppointmentEntity.class));

      // When
      Appointment result = service.cancelAppointment(TEST_BOOKING_REF, cancellationReason);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);

      verify(repository).findByBookingReference(TEST_BOOKING_REF);
      verify(validator).validateCancellable(testEntity);
      verify(repository).save(any(AppointmentEntity.class));
      verify(notificationService).sendCancellationEvent(any(AppointmentEntity.class));
    }

    @Test
    @DisplayName("Should use default reason when cancellation reason is null")
    void shouldUseDefaultReasonWhenNull() throws NotFoundException, BadRequestException {
      // Given
      String defaultReason = "Customer requested cancellation";
      AppointmentEntity cancelledEntity = testEntity.withCancellation(defaultReason, Instant.now());

      when(repository.findByBookingReference(TEST_BOOKING_REF)).thenReturn(Optional.of(testEntity));
      when(repository.save(any(AppointmentEntity.class))).thenReturn(cancelledEntity);
      when(notificationService.sendCancellationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateCancellable(any(AppointmentEntity.class));

      // When
      Appointment result = service.cancelAppointment(TEST_BOOKING_REF, defaultReason);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
      verify(repository).save(any(AppointmentEntity.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when appointment does not exist")
    void shouldThrowNotFoundWhenAppointmentDoesNotExist() {
      // Given
      when(repository.findByBookingReference(TEST_BOOKING_REF)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> service.cancelAppointment(TEST_BOOKING_REF, "reason"))
          .isInstanceOf(NotFoundException.class);

      verify(repository, never()).save(any());
      verify(notificationService, never()).sendCancellationEvent(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when appointment is not cancellable")
    void shouldThrowBadRequestWhenNotCancellable() throws BadRequestException {
      // Given
      AppointmentEntity alreadyCancelled = testEntity.withCancellation("Already cancelled",
          Instant.now());

      when(repository.findByBookingReference(TEST_BOOKING_REF)).thenReturn(
          Optional.of(alreadyCancelled));
      doThrow(new BadRequestException("Appointment is already cancelled"))
          .when(validator).validateCancellable(alreadyCancelled);

      // When/Then
      assertThatThrownBy(() -> service.cancelAppointment(TEST_BOOKING_REF, "Another reason"))
          .isInstanceOf(BadRequestException.class);

      verify(repository, never()).save(any());
      verify(notificationService, never()).sendCancellationEvent(any());
    }
  }

  @Nested
  @DisplayName("Find Appointment Tests")
  class FindAppointmentTests {

    @Test
    @DisplayName("Should find appointment by email and reference")
    void shouldFindAppointmentByEmailAndReference() {
      // Given
      String email = "john.doe@example.com";
      when(repository.findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF))
          .thenReturn(List.of(testEntity));

      // When
      Optional<Appointment> result = service.findByEmailAndReference(email, TEST_BOOKING_REF);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().bookingReference()).isEqualTo(TEST_BOOKING_REF);
      assertThat(result.get().customerEmail()).isEqualTo(email);

      verify(repository).findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF);
    }

    @Test
    @DisplayName("Should return empty when appointment not found")
    void shouldReturnEmptyWhenAppointmentNotFound() {
      // Given
      String email = "john.doe@example.com";
      when(repository.findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF))
          .thenReturn(List.of());

      // When
      Optional<Appointment> result = service.findByEmailAndReference(email, TEST_BOOKING_REF);

      // Then
      assertThat(result).isEmpty();

      verify(repository).findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF);
    }

    @Test
    @DisplayName("Should return first appointment when multiple exist")
    void shouldReturnFirstWhenMultipleExist() {
      // Given
      String email = "john.doe@example.com";
      AppointmentEntity secondEntity = new AppointmentEntity(
          2L, TEST_BOOKING_REF, 1L, "John", "Doe", email, "+27821234567",
          LocalDateTime.now().plusDays(2), 30, "Follow-up", null,
          AppointmentStatus.CONFIRMED.name(), Instant.now(), Instant.now(), null, null
      );

      when(repository.findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF))
          .thenReturn(List.of(testEntity, secondEntity));

      // When
      Optional<Appointment> result = service.findByEmailAndReference(email, TEST_BOOKING_REF);

      // Then
      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(1L);
    }
  }

  @Nested
  @DisplayName("Get Available Slots Tests")
  class GetAvailableSlotsTests {

    @Test
    @DisplayName("Should return available slots for valid branch and date")
    void shouldReturnAvailableSlots() throws NotFoundException {
      // Given
      LocalDate date = LocalDate.now().plusDays(1);
      List<TimeSlot> expectedSlots = List.of(
          new TimeSlot(
              LocalDateTime.of(date, LocalTime.of(9, 0)),
              LocalDateTime.of(date, LocalTime.of(9, 30)),
              true,
              0,
              3
          ),
          new TimeSlot(
              LocalDateTime.of(date, LocalTime.of(10, 0)),
              LocalDateTime.of(date, LocalTime.of(10, 30)),
              true,
              1,
              3
          )
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(slotAvailabilityService.getAvailableSlots(testBranch, date)).thenReturn(expectedSlots);

      // When
      List<TimeSlot> result = service.getAvailableSlots(1L, date);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(2);
      assertThat(result).isEqualTo(expectedSlots);

      verify(branchService).findById(1L);
      verify(slotAvailabilityService).getAvailableSlots(testBranch, date);
    }

    @Test
    @DisplayName("Should throw NotFoundException when branch does not exist")
    void shouldThrowNotFoundWhenBranchDoesNotExist() {
      // Given
      LocalDate tomorrow = LocalDate.now().plusDays(1);
      when(branchService.findById(1L)).thenReturn(Optional.empty());

      // When/Then
      assertThatThrownBy(() -> service.getAvailableSlots(1L, tomorrow))
          .isInstanceOf(NotFoundException.class);

      verify(slotAvailabilityService, never()).getAvailableSlots(any(), any());
    }

    @Test
    @DisplayName("Should return empty list when no slots available")
    void shouldReturnEmptyListWhenNoSlotsAvailable() throws NotFoundException {
      // Given
      LocalDate date = LocalDate.now().plusDays(1);
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(slotAvailabilityService.getAvailableSlots(testBranch, date)).thenReturn(List.of());

      // When
      List<TimeSlot> result = service.getAvailableSlots(1L, date);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isEmpty();

      verify(branchService).findById(1L);
      verify(slotAvailabilityService).getAvailableSlots(testBranch, date);
    }
  }

  @Nested
  @DisplayName("Edge Cases And Additional Coverage Tests")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle appointment with null notes")
    void shouldHandleAppointmentWithNullNotes() throws NotFoundException, BadRequestException {
      //Given
      AppointmentRequest requestWithoutNotes = new AppointmentRequest(
          1L,
          "Jane",
          "Smith",
          "jane.smith@example.com",
          "+27821234568",
          LocalDateTime.now().plusDays(1),
          30,
          "Consultation",
          null
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(requestWithoutNotes);

      //Then
      assertThat(result).isNotNull();
      assertThat(result.bookingReference()).isEqualTo(TEST_BOOKING_REF);
      verify(repository).save(any(AppointmentEntity.class));
    }

    @Test
    @DisplayName("Should handle appointment with very long duration")
    void shouldHandleVeryLongDuration() throws NotFoundException, BadRequestException {
      //Given
      AppointmentRequest longRequest = new AppointmentRequest(
          1L,
          "John",
          "Doe",
          "john.doe@example.com",
          "+27821234567",
          LocalDateTime.now().plusDays(1),
          480,
          "Full Day Session",
          "All day appointment"
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(longRequest);

      //Then
      assertThat(result).isNotNull();
      verify(validator).validateWithinOperatingHours(testBranch, longRequest.appointmentDateTime(),
          480);
    }

    @Test
    @DisplayName("Should handle appointment with minimum duration")
    void shouldHandleMinimumDuration() throws NotFoundException, BadRequestException {
      //Given
      AppointmentRequest shortRequest = new AppointmentRequest(
          1L,
          "John",
          "Doe",
          "john.doe@example.com",
          "+27821234567",
          LocalDateTime.now().plusDays(1),
          15,
          "Quick Check",
          "Brief appointment"
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(shortRequest);

      //Then
      assertThat(result).isNotNull();
      verify(validator).validateWithinOperatingHours(testBranch, shortRequest.appointmentDateTime(),
          15);
    }

    @Test
    @DisplayName("Should preserve customer name casing")
    void shouldPreserveCustomerNameCasing() throws NotFoundException, BadRequestException {
      //Given
      AppointmentRequest mixedCaseRequest = new AppointmentRequest(
          1L,
          "JoHn",
          "DoE",
          "john.doe@example.com",
          "+27821234567",
          LocalDateTime.now().plusDays(1),
          30,
          "Consultation",
          "Notes"
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      AppointmentEntity savedEntity = new AppointmentEntity(
          1L, TEST_BOOKING_REF, 1L, "JoHn", "DoE", "john.doe@example.com",
          "+27821234567", LocalDateTime.now().plusDays(1), 30, "Consultation", "Notes",
          AppointmentStatus.CONFIRMED.name(), Instant.now(), Instant.now(), null, null
      );
      when(repository.save(any(AppointmentEntity.class))).thenReturn(savedEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(mixedCaseRequest);

      //Then
      assertThat(result.customerFirstName()).isEqualTo("JoHn");
      assertThat(result.customerLastName()).isEqualTo("DoE");
    }

    @Test
    @DisplayName("Should handle appointment cancellation with custom reason")
    void shouldCancelWithCustomReason() throws NotFoundException, BadRequestException {
      //Given
      AppointmentEntity cancelledEntity = new AppointmentEntity(
          1L, TEST_BOOKING_REF, 1L, "John", "Doe", "john.doe@example.com",
          "+27821234567", LocalDateTime.now().plusDays(1), 30, "Consultation", "Notes",
          AppointmentStatus.CANCELLED.name(), Instant.now(), Instant.now(),
          Instant.now(), "Personal emergency"
      );

      when(repository.findByBookingReference(TEST_BOOKING_REF)).thenReturn(Optional.of(testEntity));
      doNothing().when(validator).validateCancellable(testEntity);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(cancelledEntity);
      when(notificationService.sendCancellationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));

      //When
      Appointment result = service.cancelAppointment(TEST_BOOKING_REF, "Personal emergency");

      //Then
      assertThat(result).isNotNull();
      assertThat(result.status()).isEqualTo(AppointmentStatus.CANCELLED);
      verify(validator).validateCancellable(testEntity);
      verify(repository).save(any(AppointmentEntity.class));
    }

    @Test
    @DisplayName("Should handle simultaneous appointment creation attempts")
    void shouldHandleDuplicateReferenceGeneration() throws NotFoundException, BadRequestException {
      // Given
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      // When
      Appointment result = service.createAppointment(validRequest);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.bookingReference()).isEqualTo(TEST_BOOKING_REF);
      verify(referenceGenerator, times(1)).generate();
    }

    @Test
    @DisplayName("Should create appointment with special characters in notes")
    void shouldHandleSpecialCharactersInNotes() throws NotFoundException, BadRequestException {
      //Given
      AppointmentRequest specialCharRequest = new AppointmentRequest(
          1L,
          "John",
          "Doe",
          "john.doe@example.com",
          "+27821234567",
          LocalDateTime.now().plusDays(1),
          30,
          "Consultation",
          "Notes with @#$%^&*()"
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(specialCharRequest);

      //Then
      assertThat(result).isNotNull();
      verify(repository).save(any(AppointmentEntity.class));
    }

    @Test
    @DisplayName("Should handle lookup with different email casing")
    void shouldLookupWithDifferentEmailCasing() {
      //Given
      String email = "JOHN.DOE@EXAMPLE.COM";
      when(repository.findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF))
          .thenReturn(List.of(testEntity));

      //When
      Optional<Appointment> result = service.findByEmailAndReference(email, TEST_BOOKING_REF);

      //Then
      assertThat(result).isPresent();
      verify(repository).findByCustomerEmailAndBookingReference(email, TEST_BOOKING_REF);
    }

    @Test
    @DisplayName("Should get available slots for far future date")
    void shouldGetAvailableSlotsForFarFutureDate() throws NotFoundException {
      //Given
      LocalDate farFuture = LocalDate.now().plusDays(365);
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      List<TimeSlot> slots = List.of(
          new TimeSlot(
              LocalDateTime.of(farFuture, LocalTime.of(9, 0)),
              LocalDateTime.of(farFuture, LocalTime.of(9, 30)),
              true, 0, 3
          )
      );
      when(slotAvailabilityService.getAvailableSlots(testBranch, farFuture)).thenReturn(slots);

      //When
      List<TimeSlot> result = service.getAvailableSlots(1L, farFuture);

      //Then
      assertThat(result).isNotNull();
      assertThat(result).hasSize(1);
      verify(branchService).findById(1L);
    }

    @Test
    @DisplayName("Should handle appointment with international phone number")
    void shouldHandleInternationalPhoneNumber() throws NotFoundException, BadRequestException {
      //Given
      AppointmentRequest internationalRequest = new AppointmentRequest(
          1L,
          "John",
          "Doe",
          "john.doe@example.com",
          "+1-555-123-4567",
          LocalDateTime.now().plusDays(1),
          30,
          "Consultation",
          "International client"
      );

      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(
          CompletableFuture.completedFuture(null));
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(internationalRequest);

      //Then
      assertThat(result).isNotNull();
      verify(repository).save(any(AppointmentEntity.class));
    }

    @Test
    @DisplayName("Should call notification service asynchronously")
    void shouldCallNotificationServiceAsync() throws NotFoundException, BadRequestException {
      //Given
      CompletableFuture<Void> future = new CompletableFuture<>();
      when(branchService.findById(1L)).thenReturn(Optional.of(testBranch));
      when(referenceGenerator.generate()).thenReturn(TEST_BOOKING_REF);
      when(repository.save(any(AppointmentEntity.class))).thenReturn(testEntity);
      when(notificationService.sendConfirmationEvent(any())).thenReturn(future);
      doNothing().when(validator).validateSlotAvailable(anyLong(), any(LocalDateTime.class));
      doNothing().when(validator)
          .validateWithinOperatingHours(any(Branch.class), any(LocalDateTime.class), anyInt());

      //When
      Appointment result = service.createAppointment(validRequest);


      //Then
      assertThat(result).isNotNull();
      verify(notificationService).sendConfirmationEvent(testEntity);
    }
  }
}
