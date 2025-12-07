package de.softwaretesting.studyconnect.exceptions;

/** Exception thrown when an internal server error occurs. */
public class InternalServerErrorException extends RuntimeException {
  public InternalServerErrorException(String message) {
    super(message);
  }
}
