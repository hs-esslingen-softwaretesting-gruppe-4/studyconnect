package de.softwaretesting.studyconnect.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("UserController Tests")
class UserControllerTest {

  @Mock private UserService userService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService)).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  @DisplayName("Should get user by ID successfully")
  void shouldGetUserByIdSuccessfully() throws Exception {
    // Given
    Long userId = 1L;
    UserResponseDTO userResponseDTO = createUserResponseDTO();
    given(userService.getUserById(userId)).willReturn(ResponseEntity.ok(userResponseDTO));

    // When & Then
    mockMvc
        .perform(get("/api/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.surname").value("John"))
        .andExpect(jsonPath("$.lastname").value("Doe"));

    verify(userService).getUserById(userId);
  }

  @Test
  @DisplayName("Should handle invalid user ID")
  void shouldHandleInvalidUserId() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/users/invalid")).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should create user successfully")
  void shouldCreateUserSuccessfully() throws Exception {
    // Given
    UserCreateRequestDTO requestDTO = createValidUserCreateRequestDTO();
    UserResponseDTO responseDTO = createUserResponseDTO();
    given(userService.createUser(any(UserCreateRequestDTO.class)))
        .willReturn(ResponseEntity.ok(responseDTO));

    // When & Then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("test@example.com"))
        .andExpect(jsonPath("$.surname").value("John"))
        .andExpect(jsonPath("$.lastname").value("Doe"));

    verify(userService).createUser(any(UserCreateRequestDTO.class));
  }

  @Test
  @DisplayName("Should fail to create user with invalid email")
  void shouldFailToCreateUserWithInvalidEmail() throws Exception {
    // Given
    UserCreateRequestDTO invalidRequestDTO = createUserCreateRequestDTOWithInvalidEmail();

    // When & Then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should fail to create user with weak password")
  void shouldFailToCreateUserWithWeakPassword() throws Exception {
    // Given
    UserCreateRequestDTO invalidRequestDTO = createUserCreateRequestDTOWithWeakPassword();

    // When & Then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should fail to create user with blank fields")
  void shouldFailToCreateUserWithBlankFields() throws Exception {
    // Given
    UserCreateRequestDTO invalidRequestDTO = createUserCreateRequestDTOWithBlankFields();

    // When & Then
    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should update user successfully")
  void shouldUpdateUserSuccessfully() throws Exception {
    // Given
    Long userId = 1L;
    UserUpdateRequestDTO requestDTO = createValidUserUpdateRequestDTO();
    UserResponseDTO responseDTO = createUpdatedUserResponseDTO();
    given(userService.updateUserWithId(eq(userId), any(UserUpdateRequestDTO.class)))
        .willReturn(ResponseEntity.ok(responseDTO));

    // When & Then
    mockMvc
        .perform(
            put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("updated@example.com"))
        .andExpect(jsonPath("$.surname").value("Jane"))
        .andExpect(jsonPath("$.lastname").value("Smith"));

    verify(userService).updateUserWithId(eq(userId), any(UserUpdateRequestDTO.class));
  }

  @Test
  @DisplayName("Should fail to update user with invalid email")
  void shouldFailToUpdateUserWithInvalidEmail() throws Exception {
    // Given
    Long userId = 1L;
    UserUpdateRequestDTO invalidRequestDTO = createUserUpdateRequestDTOWithInvalidEmail();

    // When & Then
    mockMvc
        .perform(
            put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should fail to update user with blank fields")
  void shouldFailToUpdateUserWithBlankFields() throws Exception {
    // Given
    Long userId = 1L;
    UserUpdateRequestDTO invalidRequestDTO = createUserUpdateRequestDTOWithBlankFields();

    // When & Then
    mockMvc
        .perform(
            put("/api/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  // Helper methods

  private UserResponseDTO createUserResponseDTO() {
    return new UserResponseDTO(1L, "test@example.com", "Doe", "John");
  }

  private UserResponseDTO createUpdatedUserResponseDTO() {
    return new UserResponseDTO(1L, "updated@example.com", "Smith", "Jane");
  }

  private UserCreateRequestDTO createValidUserCreateRequestDTO() {
    return new UserCreateRequestDTO("John", "Doe", "ValidPass123!", "test@example.com");
  }

  private UserCreateRequestDTO createUserCreateRequestDTOWithInvalidEmail() {
    return new UserCreateRequestDTO("John", "Doe", "ValidPass123!", "invalid-email");
  }

  private UserCreateRequestDTO createUserCreateRequestDTOWithWeakPassword() {
    return new UserCreateRequestDTO("John", "Doe", "weak", "test@example.com");
  }

  private UserCreateRequestDTO createUserCreateRequestDTOWithBlankFields() {
    return new UserCreateRequestDTO("", "", "ValidPass123!", "test@example.com");
  }

  private UserUpdateRequestDTO createValidUserUpdateRequestDTO() {
    return new UserUpdateRequestDTO("updated@example.com", "Jane", "Smith");
  }

  private UserUpdateRequestDTO createUserUpdateRequestDTOWithInvalidEmail() {
    return new UserUpdateRequestDTO("invalid-email", "Jane", "Smith");
  }

  private UserUpdateRequestDTO createUserUpdateRequestDTOWithBlankFields() {
    return new UserUpdateRequestDTO("updated@example.com", "", "");
  }
}
