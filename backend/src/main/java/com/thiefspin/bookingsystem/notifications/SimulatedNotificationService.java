package com.thiefspin.bookingsystem.notifications;

import com.thiefspin.bookingsystem.appointments.AppointmentEntity;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
    name = "notifications.mode",
    havingValue = "simulated",
    matchIfMissing = true
)
public class SimulatedNotificationService implements NotificationService {

  private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");

  @Async
  @Override
  public CompletableFuture<Void> sendConfirmationEvent(AppointmentEntity appointment) {
    return CompletableFuture.runAsync(() -> {
      sendConfirmation(appointment);
    }, executor);
  }

  @Async
  @Override
  public CompletableFuture<Void> sendCancellationEvent(AppointmentEntity appointment) {
    return CompletableFuture.runAsync(() -> {
      sendCancellation(appointment);
    }, executor);
  }

  public void sendConfirmation(AppointmentEntity appointment) {
    log.info("📧 [SIMULATED] Sending confirmation email to {}", appointment.customerEmail());
    log.info("""
        
        ╔═══════════════════════════════════════════════════════════════╗
        ║           APPOINTMENT CONFIRMATION (Simulated)                ║
        ╚═══════════════════════════════════════════════════════════════╝
        
        Dear {} {},
        
        Your appointment has been confirmed!
        
        📋 Booking Details:
           Reference: {}
           Date/Time: {}
           Duration: {} minutes{}
        
        💡 Important Information:
           • Please save your booking reference for future use
           • To cancel or modify, you'll need your email and reference
           • You will receive a reminder 24 hours before your appointment
        
        ════════════════════════════════════════════════════════════════
        """,
        appointment.customerFirstName(),
        appointment.customerLastName(),
        appointment.bookingReference(),
        appointment.appointmentDateTime().format(DATE_TIME_FORMATTER),
        appointment.durationMinutes(),
        appointment.purpose() != null ? "\n           Purpose: " + appointment.purpose() : ""
    );
  }

  public void sendCancellation(AppointmentEntity appointment) {
    log.info("📧 [SIMULATED] Sending cancellation email to {}", appointment.customerEmail());
    log.info("""
        
        ╔═══════════════════════════════════════════════════════════════╗
        ║           APPOINTMENT CANCELLATION (Simulated)                ║
        ╚═══════════════════════════════════════════════════════════════╝
        
        Dear {} {},
        
        Your appointment has been cancelled.
        
        📋 Cancellation Details:
           Reference: {}
           Originally Scheduled: {}{}
        
        💡 Next Steps:
           • You can book a new appointment at any time
           • Your booking reference is no longer valid
        
        ════════════════════════════════════════════════════════════════
        """,
        appointment.customerFirstName(),
        appointment.customerLastName(),
        appointment.bookingReference(),
        appointment.appointmentDateTime().format(DATE_TIME_FORMATTER),
        appointment.cancellationReason() != null
            ? "\n           Reason: " + appointment.cancellationReason()
            : ""
    );
  }
}
