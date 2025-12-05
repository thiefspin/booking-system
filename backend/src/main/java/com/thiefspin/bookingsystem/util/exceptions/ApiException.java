package com.thiefspin.bookingsystem.util.exceptions;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public abstract class ApiException extends Exception {
  Integer status;
  String message;
  Instant timestamp;

  public ApiException(Integer status, String message) {
    this.status = status;
    this.message = message;
    this.timestamp = Instant.now();
  }

  public @NotNull ApiErrorResponse toApiErrorResponse() {
    return new ApiErrorResponse(status, message, timestamp);
  }
}
