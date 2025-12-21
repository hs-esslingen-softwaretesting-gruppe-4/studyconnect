package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.UserRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.UserResponseMapper;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
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

    doNothing()
        .when(keycloakService)
        .createUserInRealm(anyString(), anyString(), anyString(), anyString());
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

    doThrow(new BadRequestException("KC down"))
        .when(keycloakService)
        .createUserInRealm(anyString(), anyString(), anyString(), anyString());

    assertThrows(BadRequestException.class, () -> userService.createUser(createRequest));
    verify(keycloakService).createUserInRealm(anyString(), anyString(), anyString(), anyString());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldThrowException_whenKeycloakUserRetrievalFails() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    doNothing()
        .when(keycloakService)
        .createUserInRealm(anyString(), anyString(), anyString(), anyString());
    when(keycloakService.retrieveUserByEmail("test@example.com")).thenReturn(null);

    assertThrows(InternalServerErrorException.class, () -> userService.createUser(createRequest));
    verify(keycloakService)
        .createUserInRealm("Password123!", "test@example.com", "Max", "Mustermann");
    verify(keycloakService).retrieveUserByEmail("test@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldSetKeycloakUUID() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    doNothing()
        .when(keycloakService)
        .createUserInRealm(anyString(), anyString(), anyString(), anyString());
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

    doThrow(new BadRequestException("conflict"))
        .when(keycloakService)
        .createAdminUserInRealm(anyString(), anyString(), anyString(), anyString());

    assertThrows(BadRequestException.class, () -> userService.createAdmin(createRequest));
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
        "Internal error during admin user creation in Keycloak: Cannot invoke \"de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO.getKeycloakUUID()\" because \"createdKeycloakUser\" is null",
        exception.getMessage());
    verify(keycloakService)
        .createAdminUserInRealm("AdminPass123!", "admin@example.com", "Admin", "User");
    verify(keycloakService).retrieveUserByEmail("admin@example.com");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void createUser_shouldWrapUnexpectedExceptions() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Max", "Mustermann", "Password123!", "test@example.com");

    doThrow(new RuntimeException("boom"))
        .when(keycloakService)
        .createUserInRealm(anyString(), anyString(), anyString(), anyString());

    InternalServerErrorException ex =
        assertThrows(
            InternalServerErrorException.class, () -> userService.createUser(createRequest));
    assertEquals("Internal error during user creation in Keycloak: boom", ex.getMessage());
  }

  @Test
  void createAdmin_shouldWrapUnexpectedExceptions() {
    UserCreateRequestDTO createRequest =
        new UserCreateRequestDTO("Admin", "User", "AdminPass123!", "admin@example.com");

    doThrow(new RuntimeException("boom"))
        .when(keycloakService)
        .createAdminUserInRealm(anyString(), anyString(), anyString(), anyString());

    InternalServerErrorException ex =
        assertThrows(
            InternalServerErrorException.class, () -> userService.createAdmin(createRequest));
    assertEquals("Internal error during admin user creation in Keycloak: boom", ex.getMessage());
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

  // ==================== getUserByIdEntity Tests ====================

  @Test
  void getUserByIdEntity_shouldReturnOptionalFromRepository() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    Optional<User> result = userService.getUserByIdEntity(1L);

    assertNotNull(result);
    assertEquals(true, result.isPresent());
    assertEquals(1L, result.get().getId());
    verify(userRepository).findById(1L);
  }

  // ==================== retrieveUserById Tests ====================

  @Test
  void retrieveUserById_shouldReturnUser_whenExists() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

    User result = userService.retrieveUserById(1L);

    assertNotNull(result);
    assertEquals(1L, result.getId());
    verify(userRepository).findById(1L);
  }

  @Test
  void retrieveUserById_shouldThrowNotFoundException_whenMissing() {
    when(userRepository.findById(999L)).thenReturn(Optional.empty());

    NotFoundException ex =
        assertThrows(NotFoundException.class, () -> userService.retrieveUserById(999L));

    assertEquals("User not found with id: 999", ex.getMessage());
    verify(userRepository).findById(999L);
  }

  // ==================== getUsersByIdMap Tests ====================

  @Test
  void getUsersByIdMap_shouldReturnEmptyMap_whenNullOrEmpty() {
    assertEquals(Map.of(), userService.getUsersByIdMap(null));
    assertEquals(Map.of(), userService.getUsersByIdMap(Set.of()));
  }

  @Test
  void getUsersByIdMap_shouldMapExistingUsersById_andSkipNulls() {
    User userWithId = new User();
    userWithId.setId(10L);
    userWithId.setEmail("u10@example.com");
    userWithId.setSurname("U10");
    userWithId.setLastname("User");

    User userWithoutId = new User();
    userWithoutId.setId(null);
    userWithoutId.setEmail("noid@example.com");
    userWithoutId.setSurname("No");
    userWithoutId.setLastname("Id");

    List<User> repositoryResult = new ArrayList<>();
    repositoryResult.add(userWithId);
    repositoryResult.add(userWithoutId);
    repositoryResult.add(null);

    when(userRepository.findAllById(Set.of(10L, 11L))).thenReturn(repositoryResult);

    Map<Long, User> result = userService.getUsersByIdMap(Set.of(10L, 11L));

    assertEquals(1, result.size());
    assertEquals(true, result.containsKey(10L));
    assertEquals("u10@example.com", result.get(10L).getEmail());
    verify(userRepository).findAllById(Set.of(10L, 11L));
  }

  // ==================== getUsersByIds Tests ====================

  @Test
  void getUsersByIds_shouldReturnEmptyList_whenNullOrEmpty() {
    assertEquals(List.of(), userService.getUsersByIds(null));
    assertEquals(List.of(), userService.getUsersByIds(Set.of()));
  }

  @Test
  void getUsersByIds_shouldReturnRepositoryResult_whenProvided() {
    when(userRepository.findAllById(Set.of(1L))).thenReturn(List.of(testUser));

    List<User> users = userService.getUsersByIds(Set.of(1L));

    assertEquals(1, users.size());
    assertEquals(1L, users.get(0).getId());
    verify(userRepository).findAllById(Set.of(1L));
  }

  // ==================== getUserByAccessToken Tests ====================

  @Test
  void getUserByAccessToken_shouldThrowBadRequest_whenNoJwtAuthenticationToken() {
    Authentication nonJwtAuth = org.mockito.Mockito.mock(Authentication.class);
    SecurityContextHolder.getContext().setAuthentication(nonJwtAuth);

    BadRequestException ex =
        assertThrows(BadRequestException.class, () -> userService.getUserByAccessToken());

    assertEquals("No JWT access token found", ex.getMessage());
  }

  @Test
  void getUserByAccessToken_shouldThrowBadRequest_whenSubjectBlank() {
    Jwt jwt =
        Jwt.withTokenValue("token").header("alg", "none").subject(" ").claim("sub", " ").build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(auth);

    BadRequestException ex =
        assertThrows(BadRequestException.class, () -> userService.getUserByAccessToken());

    assertEquals("Access token missing subject (sub)", ex.getMessage());
  }

  @Test
  void getUserByAccessToken_shouldReturnUser_whenUserExistsLocally() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("keycloak-uuid-123")
            .claim("sub", "keycloak-uuid-123")
            .build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userRepository.findByKeycloakUUID("keycloak-uuid-123")).thenReturn(Optional.of(testUser));
    when(userResponseMapper.toDto(testUser)).thenReturn(testUserResponseDTO);

    ResponseEntity<UserResponseDTO> response = userService.getUserByAccessToken();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("test@example.com", response.getBody().getEmail());
    verify(userRepository).findByKeycloakUUID("keycloak-uuid-123");
    verify(userResponseMapper).toDto(testUser);
  }

  @Test
  void getUserByAccessToken_shouldThrowNotFound_whenUserNotFoundLocally() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("missing-uuid")
            .claim("sub", "missing-uuid")
            .build();
    JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
    SecurityContextHolder.getContext().setAuthentication(auth);

    when(userRepository.findByKeycloakUUID("missing-uuid")).thenReturn(Optional.empty());

    NotFoundException ex =
        assertThrows(NotFoundException.class, () -> userService.getUserByAccessToken());

    assertEquals("User not found", ex.getMessage());
    verify(userRepository).findByKeycloakUUID("missing-uuid");
  }
}
