package com.thiefspin.bookingsystem.util.exceptions;

public class NotFoundException extends ClientApiException {

  public NotFoundException(String message) {
    super(404, message);
  }

}
