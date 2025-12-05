package com.thiefspin.bookingsystem.util.exceptions;

public class ClientApiException extends ApiException {

  public ClientApiException(Integer status, String message) {
    super(status, message);
  }
}
