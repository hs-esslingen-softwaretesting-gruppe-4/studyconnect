package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.UserRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.UserResponseMapper;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private UserResponseMapper userResponseMapper;

  @Mock private UserRequestMapper userRequestMapper;

  @Mock private KeycloakService keycloakService;

  @InjectMocks private UserService userService;

  private User testUser;
  private UserResponseDTO testUserResponseDTO;
  private KeycloakUserResponseDTO testKeycloakUserResponseDTO;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setKeycloakUUID("keycloak-uuid-123");
    testUser.setEmail("test@example.com");
    testUser.setSurname("Max");
    testUser.setLastname("Mustermann");
    testUser.setCreatedAt(LocalDateTime.now());

    testUserResponseDTO = new UserResponseDTO(1L, "test@example.com", "Mustermann", "Max");

    testKeycloakUserResponseDTO =
        new KeycloakUserResponseDTO(
            "keycloak-uuid-123",
            System.currentTimeMillis(),
            "test@example.com",
            true,
            true,
            "Max",
            "Mustermann",
            "test@example.com",
            null,
            null,
            null);
  }

  // ==================== getUserById Tests ====================

  @Test
  void getUserById_shouldReturnUser_whenUserExists() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userResponseMapper.toDto(testUser)).thenReturn(testUserResponseDTO);

    ResponseEntity<UserResponseDTO> response = userService.getUserById(1L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("test@example.com", response.getBody().getEmail());
    assertEquals("Max", response.getBody().getSurname());
    assertEquals("Mustermann", response.getBody().getLastname());
    verify(userRepository).findById(1L);
    verify(userResponseMapper).toDto(testUser);
  }

  @Test
  void getUserById_shouldThrowNotFoundException_whenUserDoesNotExist() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    NotFoundException exception =
        assertThrows(NotFoundException.class, () -> userService.getUserById(999L));

    assertEquals("User with id 999 not found", exception.getMessage());
    verify(userRepository).findById(999L);
    verify(userResponseMapper, never()).toDto(any(User.class));
  }

  // ==================== updateUserWithId Tests ====================

  @Test
  void updateUserWithId_shouldUpdateUser_whenUserExists() {
    UserUpdateRequestDTO updateRequest =
        new UserUpdateRequestDTO("test@example.com", "Updated", "Name");
    User updatedUser = new User();
    updatedUser.setId(1L);
    updatedUser.setSurname("Updated");
    updatedUser.setLastname("Name");
    updatedUser.setEmail("test@example.com");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRequestMapper.toEntity(any(UserUpdateRequestDTO.class))).thenReturn(updatedUser);
    when(userRepository.save(any(User.class))).thenReturn(updatedUser);
    when(userResponseMapper.toDto(any(User.class)))
        .thenReturn(new UserResponseDTO(1L, "test@example.com", "Name", "Updated"));

    ResponseEntity<UserResponseDTO> response = userService.updateUserWithId(1L, updateRequest);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Updated", response.getBody().getSurname());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void updateUserWithId_shouldThrowNotFoundException_whenUserDoesNotExist() {
    UserUpdateRequestDTO updateRequest =
        new UserUpdateRequestDTO("test@example.com", "Updated", "Name");
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> userService.updateUserWithId(999L, updateRequest));
  }

  @Test
  void updateUserWithId_shouldPreserveEmailAndCreatedAt() {
    UserUpdateRequestDTO updateRequest =
        new UserUpdateRequestDTO("newemail@example.com", "Updated", "Name");
    User mappedUser = new User();
    mappedUser.setSurname("Updated");
    mappedUser.setLastname("Name");

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(userRequestMapper.toEntity(any(UserUpdateRequestDTO.class))).thenReturn(mappedUser);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(userResponseMapper.toDto(any(User.class))).thenReturn(testUserResponseDTO);

    userService.updateUserWithId(1L, updateRequest);

    verify(userRepository)
        .save(
            argThat(
                user ->
                    user.getEmail().equals(testUser.getEmail())
                        && user.getCreatedAt().equals(testUser.getCreatedAt())));
  }

  // ==================== createUser Tests ====================

  @Test
  void createUser_shouldCreateUserSuccessfully() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    when(keycloakService.createUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(keycloakService.retrieveUserByEmail("test@example.com"))
        .thenReturn(testKeycloakUserResponseDTO);
    when(userRepository.save(any(User.class))).thenReturn(testUser);
    when(userResponseMapper.toDto(any(User.class))).thenReturn(testUserResponseDTO);

    ResponseEntity<UserResponseDTO> response = userService.createUser(createRequest);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("test@example.com", response.getBody().getEmail());
    verify(keycloakService)
        .createUserInRealm("Password123!", "test@example.com", "Max", "Mustermann");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void createUser_shouldThrowException_whenKeycloakCreationFails() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    when(keycloakService.createUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(false);

    assertThrows(InternalServerErrorException.class, () -> userService.createUser(createRequest));
    verify(keycloakService).createUserInRealm(anyString(), anyString(), anyString(), anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldThrowException_whenKeycloakUserRetrievalFails() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    when(keycloakService.createUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(keycloakService.retrieveUserByEmail("test@example.com")).thenReturn(null);

    InternalServerErrorException exception =
        assertThrows(
            InternalServerErrorException.class, () -> userService.createUser(createRequest));

    assertEquals(
        "Internal error: Error retrieving created user from Keycloak", exception.getMessage());
    verify(keycloakService)
        .createUserInRealm("Password123!", "test@example.com", "Max", "Mustermann");
    verify(keycloakService).retrieveUserByEmail("test@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldSetKeycloakUUID() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    when(keycloakService.createUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(keycloakService.retrieveUserByEmail("test@example.com"))
        .thenReturn(testKeycloakUserResponseDTO);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(userResponseMapper.toDto(any(User.class))).thenReturn(testUserResponseDTO);

    userService.createUser(createRequest);

    verify(userRepository)
        .save(argThat(user -> "keycloak-uuid-123".equals(user.getKeycloakUUID())));
  }

  // ==================== createAdmin Tests ====================

  @Test
  void createAdmin_shouldCreateAdminSuccessfully() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Admin", "User", "AdminPass123!", "admin@example.com");

    KeycloakUserResponseDTO adminKeycloakUser =
        new KeycloakUserResponseDTO(
            "admin-keycloak-uuid",
            System.currentTimeMillis(),
            "admin@example.com",
            true,
            true,
            "Admin",
            "User",
            "admin@example.com",
            null,
            null,
            null);

    when(keycloakService.createAdminUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(keycloakService.retrieveUserByEmail("admin@example.com")).thenReturn(adminKeycloakUser);
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    when(userResponseMapper.toDto(any(User.class)))
        .thenReturn(new UserResponseDTO(2L, "admin@example.com", "User", "Admin"));

    ResponseEntity<UserResponseDTO> response = userService.createAdmin(createRequest);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    verify(keycloakService)
        .createAdminUserInRealm("AdminPass123!", "admin@example.com", "Admin", "User");
    verify(userRepository).save(any(User.class));
  }

  @Test
  void createAdmin_shouldThrowException_whenKeycloakAdminCreationFails() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Admin", "User", "AdminPass123!", "admin@example.com");

    when(keycloakService.createAdminUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(false);

    assertThrows(InternalServerErrorException.class, () -> userService.createAdmin(createRequest));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createAdmin_shouldThrowException_whenKeycloakAdminRetrievalFails() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Admin", "User", "AdminPass123!", "admin@example.com");

    when(keycloakService.createAdminUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(keycloakService.retrieveUserByEmail("admin@example.com")).thenReturn(null);

    InternalServerErrorException exception =
        assertThrows(
            InternalServerErrorException.class, () -> userService.createAdmin(createRequest));

    assertEquals(
        "Internal error: Error retrieving created admin from Keycloak", exception.getMessage());
    verify(keycloakService)
        .createAdminUserInRealm("AdminPass123!", "admin@example.com", "Admin", "User");
    verify(keycloakService).retrieveUserByEmail("admin@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldHandleRetrievalFailureAfterMultipleRetries() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    when(keycloakService.createUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    // Simulate multiple calls returning null (as if retries were implemented)
    when(keycloakService.retrieveUserByEmail("test@example.com")).thenReturn(null);

    InternalServerErrorException exception =
        assertThrows(
            InternalServerErrorException.class, () -> userService.createUser(createRequest));

    assertEquals(
        "Internal error: Error retrieving created user from Keycloak", exception.getMessage());
    verify(keycloakService).retrieveUserByEmail("test@example.com");
  }

  @Test
  void createAdmin_shouldHandleRetrievalFailureAfterSuccessfulCreation() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("SuperAdmin", "User", "AdminPass123!", "super@admin.com");

    when(keycloakService.createAdminUserInRealm(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(keycloakService.retrieveUserByEmail("super@admin.com")).thenReturn(null);

    InternalServerErrorException exception =
        assertThrows(
            InternalServerErrorException.class, () -> userService.createAdmin(createRequest));

    assertEquals(
        "Internal error: Error retrieving created admin from Keycloak", exception.getMessage());
    verify(keycloakService)
        .createAdminUserInRealm("AdminPass123!", "super@admin.com", "SuperAdmin", "User");
    verify(userRepository, never()).save(any(User.class));
  }

  // ==================== init Tests ====================

  @Test
  void init_shouldCreateRealmAndSyncUsers() {
    when(keycloakService.createRealm()).thenReturn(true);
    when(keycloakService.retrieveAllUsersInRealm())
        .thenReturn(List.of(testKeycloakUserResponseDTO));
    when(userRepository.findByKeycloakUUID("keycloak-uuid-123")).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    userService.init();

    verify(keycloakService).createRealm();
    verify(keycloakService).retrieveAllUsersInRealm();
    verify(userRepository).save(any(User.class));
  }

  @Test
  void init_shouldNotSyncUsers_whenRealmCreationFails() {
    when(keycloakService.createRealm()).thenReturn(false);

    userService.init();

    verify(keycloakService).createRealm();
    verify(keycloakService, never()).retrieveAllUsersInRealm();
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void init_shouldSkipExistingUsers() {
    when(keycloakService.createRealm()).thenReturn(true);
    when(keycloakService.retrieveAllUsersInRealm())
        .thenReturn(List.of(testKeycloakUserResponseDTO));
    when(userRepository.findByKeycloakUUID("keycloak-uuid-123")).thenReturn(Optional.of(testUser));

    userService.init();

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void init_shouldHandleEmptyUserList() {
    when(keycloakService.createRealm()).thenReturn(true);
    when(keycloakService.retrieveAllUsersInRealm()).thenReturn(Collections.emptyList());

    userService.init();

    verify(keycloakService).retrieveAllUsersInRealm();
    verify(userRepository, never()).save(any(User.class));
  }
}
