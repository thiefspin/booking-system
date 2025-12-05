package com.thiefspin.bookingsystem;

import com.thiefspin.bookingsystem.appointments.AppointmentRepository;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BookingReferenceGenerator {

  private static final String PREFIX = "BK";
  private static final int MAX_ATTEMPTS = 5;

  private AppointmentRepository repository;

  public String generate() {
    return Stream.generate(this::generateRandom)
        .limit(MAX_ATTEMPTS)
        .filter(this::isReferenceUnique)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Failed to generate unique reference"
        ));
  }

  private String generateRandom() {
    String uuid = UUID.randomUUID().toString().replace("-", "").toUpperCase();
    return PREFIX + uuid.substring(0, 8);
  }

  private boolean isReferenceUnique(String reference) {
    return repository.findByBookingReference(reference).isEmpty();
  }
}

