package de.softwaretesting.studyconnect.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Comprehensive tests for GlobalExceptionHandler. */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private HttpServletRequest request;

  private GlobalExceptionHandler globalExceptionHandler;

  @BeforeEach
  void setUp() {
    globalExceptionHandler = new GlobalExceptionHandler();
  }

  @Test
  void handleNotFoundException_ShouldReturnNotFoundResponse() {
    // Given
    String errorMessage = "User not found with id: 123";
    String requestPath = "/api/users/123";
    NotFoundException exception = new NotFoundException(errorMessage);

    when(request.getRequestURI()).thenReturn(requestPath);

    // When
    ResponseEntity<Map<String, Object>> response =
        globalExceptionHandler.handleNotFoundException(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    Map<String, Object> body = response.getBody();
    assertThat(body)
        .isNotNull()
        .containsEntry("timestamp", body.get("timestamp"))
        .containsEntry("status", 404)
        .containsEntry("error", "Not Found")
        .containsEntry("message", errorMessage)
        .containsEntry("path", requestPath);
    assertThat(body.get("timestamp")).isInstanceOf(Instant.class);
  }

  @Test
  void handleBadRequestException_ShouldReturnBadRequestResponse() {
    // Given
    String errorMessage = "Invalid request parameters";
    String requestPath = "/api/users";
    BadRequestException exception = new BadRequestException(errorMessage);

    when(request.getRequestURI()).thenReturn(requestPath);

    // When
    ResponseEntity<Map<String, Object>> response =
        globalExceptionHandler.handleBadRequestException(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> body = response.getBody();
    assertThat(body)
        .isNotNull()
        .containsEntry("status", 400)
        .containsEntry("error", "Bad Request")
        .containsEntry("message", errorMessage)
        .containsEntry("path", requestPath);
    assertThat(body.get("timestamp")).isInstanceOf(Instant.class);
  }

  @Test
  void handleInternalServerErrorException_ShouldReturnInternalServerErrorResponse() {
    // Given
    String errorMessage = "Database connection failed";
    String requestPath = "/api/tasks";
    InternalServerErrorException exception = new InternalServerErrorException(errorMessage);

    when(request.getRequestURI()).thenReturn(requestPath);

    // When
    ResponseEntity<Map<String, Object>> response =
        globalExceptionHandler.handleInternalServerErrorException(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    Map<String, Object> body = response.getBody();
    assertThat(body)
        .isNotNull()
        .containsEntry("status", 500)
        .containsEntry("error", "Internal Server Error")
        .containsEntry("message", errorMessage)
        .containsEntry("path", requestPath);
    assertThat(body.get("timestamp")).isInstanceOf(Instant.class);
  }

  @Test
  void handleValidationException_ShouldReturnBadRequestWithFormattedErrors() {
    // Given
    String requestPath = "/api/users";
    when(request.getRequestURI()).thenReturn(requestPath);

    // Create a mock MethodArgumentNotValidException
    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "testObject");
    bindingResult.addError(new FieldError("testObject", "name", "must not be null"));
    bindingResult.addError(new FieldError("testObject", "email", "must be a valid email"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    // When
    ResponseEntity<Map<String, Object>> response =
        globalExceptionHandler.handleValidationException(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> body = response.getBody();
    assertThat(body)
        .isNotNull()
        .containsEntry("status", 400)
        .containsEntry("error", "Validation Failed")
        .containsEntry("path", requestPath);
    assertThat(body.get("timestamp")).isInstanceOf(Instant.class);

    assertThat((String) body.get("message"))
        .contains("name: must not be null")
        .contains("email: must be a valid email");
  }

  @Test
  void handleValidationException_ShouldHandleSingleFieldError() {
    // Given
    String requestPath = "/api/users";
    when(request.getRequestURI()).thenReturn(requestPath);

    BeanPropertyBindingResult bindingResult =
        new BeanPropertyBindingResult(new Object(), "testObject");
    bindingResult.addError(new FieldError("testObject", "name", "must not be empty"));

    MethodArgumentNotValidException exception =
        new MethodArgumentNotValidException(null, bindingResult);

    // When
    ResponseEntity<Map<String, Object>> response =
        globalExceptionHandler.handleValidationException(exception, request);

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    Map<String, Object> body = response.getBody();
    assertThat(body).containsEntry("message", "name: must not be empty");
  }

  @Test
  void exceptionHandlers_ShouldIncludeTimestampInResponse() {
    // Given
    when(request.getRequestURI()).thenReturn("/test");
    Instant beforeCall = Instant.now();

    // When
    ResponseEntity<Map<String, Object>> notFoundResponse =
        globalExceptionHandler.handleNotFoundException(new NotFoundException("test"), request);
    ResponseEntity<Map<String, Object>> badRequestResponse =
        globalExceptionHandler.handleBadRequestException(new BadRequestException("test"), request);
    ResponseEntity<Map<String, Object>> serverErrorResponse =
        globalExceptionHandler.handleInternalServerErrorException(
            new InternalServerErrorException("test"), request);

    Instant afterCall = Instant.now();

    // Then
    List<ResponseEntity<Map<String, Object>>> responses =
        List.of(notFoundResponse, badRequestResponse, serverErrorResponse);

    for (ResponseEntity<Map<String, Object>> response : responses) {
      Instant timestamp = (Instant) response.getBody().get("timestamp");
      assertThat(timestamp).isBetween(beforeCall, afterCall);
    }
  }

  @Test
  void exceptionHandlers_ShouldHandleNullMessages() {
    // Given
    when(request.getRequestURI()).thenReturn("/test");

    // When
    ResponseEntity<Map<String, Object>> notFoundResponse =
        globalExceptionHandler.handleNotFoundException(new NotFoundException(null), request);
    ResponseEntity<Map<String, Object>> badRequestResponse =
        globalExceptionHandler.handleBadRequestException(new BadRequestException(null), request);
    ResponseEntity<Map<String, Object>> serverErrorResponse =
        globalExceptionHandler.handleInternalServerErrorException(
            new InternalServerErrorException(null), request);

    // Then
    assertThat(notFoundResponse.getBody().get("message")).isNull();
    assertThat(badRequestResponse.getBody().get("message")).isNull();
    assertThat(serverErrorResponse.getBody().get("message")).isNull();
  }

  /**
   * Integration tests that verify the GlobalExceptionHandler works correctly in a Spring MVC
   * context.
   */
  @WebMvcTest(TestController.class)
  @ActiveProfiles("test")
  @Import({GlobalExceptionHandler.class, TestConfiguration.class})
  static class GlobalExceptionHandlerIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void notFoundException_ShouldReturn404WithCorrectBody() throws Exception {
      mockMvc
          .perform(get("/test/not-found"))
          .andExpect(status().isNotFound())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status").value(404))
          .andExpect(jsonPath("$.error").value("Not Found"))
          .andExpect(jsonPath("$.message").value("Resource not found"))
          .andExpect(jsonPath("$.path").value("/test/not-found"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void badRequestException_ShouldReturn400WithCorrectBody() throws Exception {
      mockMvc
          .perform(get("/test/bad-request"))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Bad Request"))
          .andExpect(jsonPath("$.message").value("Invalid parameters"))
          .andExpect(jsonPath("$.path").value("/test/bad-request"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void internalServerErrorException_ShouldReturn500WithCorrectBody() throws Exception {
      mockMvc
          .perform(get("/test/server-error"))
          .andExpect(status().isInternalServerError())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status").value(500))
          .andExpect(jsonPath("$.error").value("Internal Server Error"))
          .andExpect(jsonPath("$.message").value("Something went wrong"))
          .andExpect(jsonPath("$.path").value("/test/server-error"))
          .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void validationException_ShouldReturn400WithFormattedErrors() throws Exception {
      // Create invalid request body
      String invalidJson = "{\"name\": \"\", \"email\": \"invalid-email\"}";

      mockMvc
          .perform(
              post("/test/validate").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
          .andExpect(status().isBadRequest())
          .andExpect(content().contentType(MediaType.APPLICATION_JSON))
          .andExpect(jsonPath("$.status").value(400))
          .andExpect(jsonPath("$.error").value("Validation Failed"))
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.path").value("/test/validate"))
          .andExpect(jsonPath("$.timestamp").exists());
    }
  }

  /** Test controller for integration tests */
  @RestController
  static class TestController {

    @GetMapping("/test/not-found")
    public String triggerNotFoundException() {
      throw new NotFoundException("Resource not found");
    }

    @GetMapping("/test/bad-request")
    public String triggerBadRequestException() {
      throw new BadRequestException("Invalid parameters");
    }

    @GetMapping("/test/server-error")
    public String triggerServerErrorException() {
      throw new InternalServerErrorException("Something went wrong");
    }

    @PostMapping("/test/validate")
    public String triggerValidationException(@RequestBody TestDto testDto) {
      return "success";
    }
  }

  /** Test DTO for validation tests */
  static class TestDto {
    private String name;
    private String email;
  }
}
