package com.thiefspin.bookingsystem.util.exceptions;

public class BadRequestException extends ClientApiException {

  public BadRequestException(String message) {
    super(400, message);
  }

}
