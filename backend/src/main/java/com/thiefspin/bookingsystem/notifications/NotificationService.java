package com.thiefspin.bookingsystem.notifications;

import com.thiefspin.bookingsystem.appointments.AppointmentEntity;
import java.util.concurrent.CompletableFuture;

public interface NotificationService {

  CompletableFuture<Void> sendConfirmationEvent(AppointmentEntity appointment);

  CompletableFuture<Void> sendCancellationEvent(AppointmentEntity appointment);

}
