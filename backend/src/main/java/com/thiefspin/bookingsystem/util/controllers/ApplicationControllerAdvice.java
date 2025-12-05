package com.thiefspin.bookingsystem.util.controllers;

import com.thiefspin.bookingsystem.util.exceptions.ApiErrorResponse;
import com.thiefspin.bookingsystem.util.exceptions.ClientApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class ApplicationControllerAdvice {

  @ExceptionHandler(ClientApiException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ClientApiException ex,
      HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.toApiErrorResponse());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
      HttpServletRequest request) {

    String message = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .collect(Collectors.joining("; "));

    ApiErrorResponse body = new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        message,
        Instant.now()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentTypeMismatchException ex,
      HttpServletRequest request) {

    ApiErrorResponse body = new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        Instant.now()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MissingServletRequestParameterException ex,
      HttpServletRequest request) {

    ApiErrorResponse body = new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        ex.getMessage(),
        Instant.now()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
      HttpServletRequest request) {
    String message = ex.getConstraintViolations()
        .stream()
        .map(v -> v.getPropertyPath() + " " + v.getMessage())
        .collect(Collectors.joining("; "));

    ApiErrorResponse body = new ApiErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        message,
        Instant.now()
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex,
      HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        new ApiErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred.",
            java.time.Instant.now()
        )
    );
  }
}
