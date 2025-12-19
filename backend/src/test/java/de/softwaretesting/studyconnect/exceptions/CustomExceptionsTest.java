package de.softwaretesting.studyconnect.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for custom exception classes. */
class CustomExceptionsTest {

  @Test
  void notFoundExceptionConstructor_ShouldSetMessageCorrectly() {
    // Given
    String message = "User not found with id: 123";

    // When
    NotFoundException exception = new NotFoundException(message);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void notFoundExceptionConstructor_ShouldHandleNullMessage() {
    // When
    NotFoundException exception = new NotFoundException(null);

    // Then
    assertThat(exception.getMessage()).isNull();
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void badRequestExceptionConstructor_ShouldSetMessageCorrectly() {
    // Given
    String message = "Invalid request parameters";

    // When
    BadRequestException exception = new BadRequestException(message);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void badRequestExceptionConstructor_ShouldHandleNullMessage() {
    // When
    BadRequestException exception = new BadRequestException(null);

    // Then
    assertThat(exception.getMessage()).isNull();
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void internalServerErrorExceptionConstructor_ShouldSetMessageCorrectly() {
    // Given
    String message = "Database connection failed";

    // When
    InternalServerErrorException exception = new InternalServerErrorException(message);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void internalServerErrorExceptionConstructor_ShouldHandleNullMessage() {
    // When
    InternalServerErrorException exception = new InternalServerErrorException(null);

    // Then
    assertThat(exception.getMessage()).isNull();
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void keycloakTokenFetchExceptionConstructor_ShouldSetMessageCorrectly() {
    // Given
    String message = "Failed to fetch Keycloak token";

    // When
    KeycloakTokenFetchException exception = new KeycloakTokenFetchException(message);

    // Then
    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void keycloakTokenFetchExceptionConstructor_ShouldHandleNullMessage() {
    // When
    KeycloakTokenFetchException exception = new KeycloakTokenFetchException(null);

    // Then
    assertThat(exception.getMessage()).isNull();
    assertThat(exception).isInstanceOf(RuntimeException.class);
  }

  @Test
  void exceptionInheritance_ShouldExtendRuntimeException() {
    // Given & When & Then
    assertThat(new NotFoundException("test")).isInstanceOf(RuntimeException.class);
    assertThat(new BadRequestException("test")).isInstanceOf(RuntimeException.class);
    assertThat(new InternalServerErrorException("test")).isInstanceOf(RuntimeException.class);
    assertThat(new KeycloakTokenFetchException("test")).isInstanceOf(RuntimeException.class);
  }

  @Test
  void exceptionMessages_ShouldBeAccessibleViaGetMessage() {
    // Given
    String testMessage = "Test message";

    // When
    NotFoundException notFound = new NotFoundException(testMessage);
    BadRequestException badRequest = new BadRequestException(testMessage);
    InternalServerErrorException serverError = new InternalServerErrorException(testMessage);
    KeycloakTokenFetchException keycloakException = new KeycloakTokenFetchException(testMessage);

    // Then
    assertThat(notFound.getMessage()).isEqualTo(testMessage);
    assertThat(badRequest.getMessage()).isEqualTo(testMessage);
    assertThat(serverError.getMessage()).isEqualTo(testMessage);
    assertThat(keycloakException.getMessage()).isEqualTo(testMessage);
  }
}
