package com.thiefspin.bookingsystem.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiefspin.bookingsystem.appointments.Appointment;
import com.thiefspin.bookingsystem.appointments.AppointmentService;
import com.thiefspin.bookingsystem.appointments.AppointmentStatus;
import com.thiefspin.bookingsystem.appointments.requests.AppointmentRequest;
import com.thiefspin.bookingsystem.appointments.slots.TimeSlot;
import com.thiefspin.bookingsystem.util.exceptions.BadRequestException;
import com.thiefspin.bookingsystem.util.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AppointmentController.class)
@ActiveProfiles("unit")
@DisplayName("AppointmentController Tests")
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    private AppointmentRequest validRequest;
    private Appointment testAppointment;
    private final String TEST_BOOKING_REF = "BK12345678";

    @BeforeEach
    void setUp() {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(1);

        validRequest = new AppointmentRequest(
            1L,
            "John",
            "Doe",
            "john.doe@example.com",
            "+27821234567",
            appointmentTime,
            30,
            "Consultation",
            "Please call me 5 minutes before"
        );

        testAppointment = new Appointment(
            1L,
            TEST_BOOKING_REF,
            1L,
            "John",
            "Doe",
            "john.doe@example.com",
            "+27821234567",
            appointmentTime,
            30,
            "Consultation",
            "Please call me 5 minutes before",
            AppointmentStatus.CONFIRMED
        );
    }

    @Nested
    @DisplayName("GET /api/appointments/slots")
    class GetAvailableSlotsTests {

        @Test
        @DisplayName("Should return available slots for valid branch and date")
        void shouldReturnAvailableSlots() throws Exception {
            // Given
            LocalDate date = LocalDate.now().plusDays(1);
            List<TimeSlot> slots = List.of(
                new TimeSlot(
                    LocalDateTime.of(date, java.time.LocalTime.of(9, 0)),
                    LocalDateTime.of(date, java.time.LocalTime.of(9, 30)),
                    true,
                    0,
                    3
                ),
                new TimeSlot(
                    LocalDateTime.of(date, java.time.LocalTime.of(10, 0)),
                    LocalDateTime.of(date, java.time.LocalTime.of(10, 30)),
                    true,
                    1,
                    3
                )
            );

            when(appointmentService.getAvailableSlots(1L, date)).thenReturn(slots);

            // When & Then
            mockMvc.perform(get("/api/appointments/slots")
                    .param("branchId", "1")
                    .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].available").value(true))
                .andExpect(jsonPath("$[0].currentBookings").value(0))
                .andExpect(jsonPath("$[0].maxBookings").value(3))
                .andExpect(jsonPath("$[1].currentBookings").value(1));

            verify(appointmentService).getAvailableSlots(1L, date);
        }

        @Test
        @DisplayName("Should return 404 when branch not found")
        void shouldReturn404WhenBranchNotFound() throws Exception {
            // Given
            LocalDate date = LocalDate.now().plusDays(1);
            when(appointmentService.getAvailableSlots(1L, date))
                .thenThrow(new NotFoundException("Branch not found"));

            // When & Then
            mockMvc.perform(get("/api/appointments/slots")
                    .param("branchId", "1")
                    .param("date", date.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

            verify(appointmentService).getAvailableSlots(1L, date);
        }

        @Test
        @DisplayName("Should return 400 when required date parameter is missing")
        void shouldReturn400WhenDateParameterMissing() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/appointments/slots")
                    .param("branchId", "1"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(appointmentService);
        }

        @Test
        @DisplayName("Should return 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/appointments/slots")
                    .param("branchId", "1")
                    .param("date", "invalid-date"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(appointmentService);
        }
    }

    @Nested
    @DisplayName("POST /api/appointments/book")
    class CreateAppointmentTests {

        @Test
        @DisplayName("Should create appointment successfully")
        void shouldCreateAppointmentSuccessfully() throws Exception {
            // Given
            when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenReturn(testAppointment);

            // When & Then
            mockMvc.perform(post("/api/appointments/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingReference").value(TEST_BOOKING_REF))
                .andExpect(jsonPath("$.customerFirstName").value("John"))
                .andExpect(jsonPath("$.customerLastName").value("Doe"))
                .andExpect(jsonPath("$.customerEmail").value("john.doe@example.com"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

            verify(appointmentService).createAppointment(any(AppointmentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid request data")
        void shouldReturn400ForInvalidRequest() throws Exception {
            // Given
            String invalidJson = """
                {
                    "branchId": null,
                    "firstName": "",
                    "lastName": "Doe",
                    "email": "invalid-email",
                    "phoneNumber": "123",
                    "appointmentDateTime": "%s",
                    "durationMinutes": 10,
                    "purpose": null,
                    "notes": null
                }
                """.formatted(LocalDateTime.now().minusDays(1));

            // When & Then
            mockMvc.perform(post("/api/appointments/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when branch not found during booking")
        void shouldReturn404WhenBranchNotFound() throws Exception {
            // Given
            when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenThrow(new NotFoundException("Branch not found"));

            // When & Then
            mockMvc.perform(post("/api/appointments/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

            verify(appointmentService).createAppointment(any(AppointmentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when slot not available")
        void shouldReturn400WhenSlotNotAvailable() throws Exception {
            // Given
            when(appointmentService.createAppointment(any(AppointmentRequest.class)))
                .thenThrow(new BadRequestException("Slot not available"));

            // When & Then
            mockMvc.perform(post("/api/appointments/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

            verify(appointmentService).createAppointment(any(AppointmentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        void shouldReturn400ForInvalidEmailFormat() throws Exception {
            // Given
            String invalidEmailRequest = """
                {
                    "branchId": 1,
                    "firstName": "John",
                    "lastName": "Doe",
                    "email": "not-an-email",
                    "phoneNumber": "+27821234567",
                    "appointmentDateTime": "%s",
                    "durationMinutes": 30,
                    "purpose": "Consultation",
                    "notes": null
                }
                """.formatted(LocalDateTime.now().plusDays(1));

            // When & Then
            mockMvc.perform(post("/api/appointments/book")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidEmailRequest))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(appointmentService);
        }
    }

    @Nested
    @DisplayName("GET /api/appointments/lookup")
    class LookupAppointmentTests {

        @Test
        @DisplayName("Should return appointment when found")
        void shouldReturnAppointmentWhenFound() throws Exception {
            // Given
            String email = "john.doe@example.com";
            when(appointmentService.findByEmailAndReference(email, TEST_BOOKING_REF))
                .thenReturn(Optional.of(testAppointment));

            // When & Then
            mockMvc.perform(get("/api/appointments/lookup")
                    .param("email", email)
                    .param("bookingReference", TEST_BOOKING_REF))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingReference").value(TEST_BOOKING_REF))
                .andExpect(jsonPath("$.customerEmail").value(email));

            verify(appointmentService).findByEmailAndReference(email, TEST_BOOKING_REF);
        }

        @Test
        @DisplayName("Should return 404 when appointment not found")
        void shouldReturn404WhenAppointmentNotFound() throws Exception {
            // Given
            String email = "john.doe@example.com";
            when(appointmentService.findByEmailAndReference(email, TEST_BOOKING_REF))
                .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/appointments/lookup")
                    .param("email", email)
                    .param("bookingReference", TEST_BOOKING_REF))
                .andExpect(status().isNotFound());

            verify(appointmentService).findByEmailAndReference(email, TEST_BOOKING_REF);
        }

        @Test
        @DisplayName("Should return 400 when required reference parameter is missing")
        void shouldReturn400WhenReferenceParameterMissing() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/appointments/lookup")
                    .param("email", "john@example.com"))
                .andExpect(status().isBadRequest());

            verifyNoInteractions(appointmentService);
        }
    }

    @Nested
    @DisplayName("PUT /api/appointments/cancel")
    class CancelAppointmentTests {

        @Test
        @DisplayName("Should cancel appointment successfully")
        void shouldCancelAppointmentSuccessfully() throws Exception {
            // Given
            String email = "john.doe@example.com";
            String reason = "Unable to attend";
            Appointment cancelledAppointment = new Appointment(
                testAppointment.id(),
                testAppointment.bookingReference(),
                testAppointment.branchId(),
                testAppointment.customerFirstName(),
                testAppointment.customerLastName(),
                testAppointment.customerEmail(),
                testAppointment.customerPhone(),
                testAppointment.appointmentDateTime(),
                testAppointment.durationMinutes(),
                testAppointment.purpose(),
                testAppointment.notes(),
                AppointmentStatus.CANCELLED
            );

            when(appointmentService.findByEmailAndReference(email, TEST_BOOKING_REF))
                .thenReturn(Optional.of(testAppointment));
            when(appointmentService.cancelAppointment(TEST_BOOKING_REF, reason))
                .thenReturn(cancelledAppointment);

            // When & Then
            mockMvc.perform(put("/api/appointments/cancel")
                    .param("email", email)
                    .param("bookingReference", TEST_BOOKING_REF)
                    .param("reason", reason))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.bookingReference").value(TEST_BOOKING_REF))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

            verify(appointmentService).findByEmailAndReference(email, TEST_BOOKING_REF);
            verify(appointmentService).cancelAppointment(TEST_BOOKING_REF, reason);
        }

        @Test
        @DisplayName("Should cancel with default reason when reason not provided")
        void shouldCancelWithDefaultReason() throws Exception {
            // Given
            String email = "john.doe@example.com";
            Appointment cancelledAppointment = new Appointment(
                testAppointment.id(),
                testAppointment.bookingReference(),
                testAppointment.branchId(),
                testAppointment.customerFirstName(),
                testAppointment.customerLastName(),
                testAppointment.customerEmail(),
                testAppointment.customerPhone(),
                testAppointment.appointmentDateTime(),
                testAppointment.durationMinutes(),
                testAppointment.purpose(),
                testAppointment.notes(),
                AppointmentStatus.CANCELLED
            );

            when(appointmentService.findByEmailAndReference(email, TEST_BOOKING_REF))
                .thenReturn(Optional.of(testAppointment));
            when(appointmentService.cancelAppointment(eq(TEST_BOOKING_REF), anyString()))
                .thenReturn(cancelledAppointment);

            // When & Then
            mockMvc.perform(put("/api/appointments/cancel")
                    .param("email", email)
                    .param("bookingReference", TEST_BOOKING_REF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

            verify(appointmentService).cancelAppointment(TEST_BOOKING_REF, "Customer requested cancellation");
        }

        @Test
        @DisplayName("Should return 404 when appointment not found")
        void shouldReturn404WhenAppointmentNotFound() throws Exception {
            // Given
            String email = "john.doe@example.com";
            when(appointmentService.findByEmailAndReference(email, TEST_BOOKING_REF))
                .thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(put("/api/appointments/cancel")
                    .param("email", email)
                    .param("bookingReference", TEST_BOOKING_REF))
                .andExpect(status().isNotFound());

            verify(appointmentService).findByEmailAndReference(email, TEST_BOOKING_REF);
            verify(appointmentService, never()).cancelAppointment(anyString(), anyString());
        }

        @Test
        @DisplayName("Should return 400 when appointment cannot be cancelled")
        void shouldReturn400WhenCannotCancel() throws Exception {
            // Given
            String email = "john.doe@example.com";
            when(appointmentService.findByEmailAndReference(email, TEST_BOOKING_REF))
                .thenReturn(Optional.of(testAppointment));
            when(appointmentService.cancelAppointment(eq(TEST_BOOKING_REF), anyString()))
                .thenThrow(new BadRequestException("Appointment is already cancelled"));

            // When & Then
            mockMvc.perform(put("/api/appointments/cancel")
                    .param("email", email)
                    .param("bookingReference", TEST_BOOKING_REF))
                .andExpect(status().isBadRequest());

            verify(appointmentService).cancelAppointment(eq(TEST_BOOKING_REF), anyString());
        }
    }
}
