package de.softwaretesting.studyconnect.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for handling custom exceptions and returning appropriate HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private ResponseEntity<Map<String, Object>> createResponseEntity(
      HttpStatus status, String error, String message, HttpServletRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", status.value());
    body.put("error", error);
    body.put("message", message);
    body.put("path", request.getRequestURI());

    return new ResponseEntity<>(body, status);
  }

  @ExceptionHandler(NotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<Map<String, Object>> handleNotFoundException(
      NotFoundException ex, HttpServletRequest request) {
    return createResponseEntity(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
  }

  @ExceptionHandler(BadRequestException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Map<String, Object>> handleBadRequestException(
      BadRequestException ex, HttpServletRequest request) {
    return createResponseEntity(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
  }

  @ExceptionHandler(InternalServerErrorException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Map<String, Object>> handleInternalServerErrorException(
      InternalServerErrorException ex, HttpServletRequest request) {
    return createResponseEntity(
        HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String message =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
    return createResponseEntity(HttpStatus.BAD_REQUEST, "Validation Failed", message, request);
  }

  @ExceptionHandler(KeycloakTokenFetchException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<Map<String, Object>> handleKeycloakTokenFetchException(
      KeycloakTokenFetchException ex, HttpServletRequest request) {

    throw new InternalServerErrorException(
        "An internal error occurred while fetching the Keycloak token.");
  }
}
