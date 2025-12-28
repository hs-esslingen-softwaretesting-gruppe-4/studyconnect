package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class KeycloakServiceTest {

  @Mock private KeycloakAdminTokenService keycloakAdminTokenService;

  @Mock private RestTemplate restTemplate;

  @InjectMocks private KeycloakService keycloakService;

  private static final String TEST_TOKEN = "test-access-token";
  private static final String TEST_KEYCLOAK_URL = "http://localhost:8080";
  private static final String TEST_REALM = "test-realm";
  private static final String TEST_CLIENT_ROLE = "user";
  private static final String TEST_ADMIN_ROLE = "admin";

  @BeforeEach
  void setUp() throws Exception {
    // Set up @Value fields using reflection
    setField(keycloakService, "keycloakServerUrl", TEST_KEYCLOAK_URL);
    setField(keycloakService, "realmName", TEST_REALM);
    setField(keycloakService, "defaultClientRole", TEST_CLIENT_ROLE);
    setField(keycloakService, "defaultAdminRole", TEST_ADMIN_ROLE);

    // Mock the token service to return a valid token
    lenient().when(keycloakAdminTokenService.getAccessToken()).thenReturn(TEST_TOKEN);
  }

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  // ========== createRealm() tests ==========

  @Test
  @SuppressWarnings("unchecked")
  void createRealm_shouldReturnTrue_whenRealmAlreadyExists() {
    // Mock getAllRealms to return a list containing our realm
    Map<String, Object>[] realmsArray =
        new Map[] {Map.of("realm", TEST_REALM), Map.of("realm", "other-realm")};
    ResponseEntity<Map[]> realmsResponse = new ResponseEntity<>(realmsArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map[].class)))
        .thenReturn(realmsResponse);

    boolean result = keycloakService.createRealm();

    assertTrue(result);
    // Verify that no POST was made to create a realm
    verify(restTemplate, never())
        .postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"), any(HttpEntity.class), eq(Void.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void createRealm_shouldCreateRealm_whenRealmDoesNotExist() {
    // Mock getAllRealms to return empty list
    Map<String, Object>[] realmsArray = new Map[] {};
    ResponseEntity<Map[]> realmsResponse = new ResponseEntity<>(realmsArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map[].class)))
        .thenReturn(realmsResponse);

    // Mock realm creation
    ResponseEntity<Void> createResponse = new ResponseEntity<>(HttpStatus.CREATED);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"), any(HttpEntity.class), eq(Void.class)))
        .thenReturn(createResponse);

    boolean result = keycloakService.createRealm();

    assertTrue(result);
    // Verify realm creation was called
    verify(restTemplate)
        .postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"), any(HttpEntity.class), eq(Void.class));
    verify(restTemplate, never())
        .postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/roles"),
            any(HttpEntity.class),
            eq(Void.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void createRealm_shouldReturnFalse_whenCreationFails() {
    // Mock getAllRealms to return empty list
    Map<String, Object>[] realmsArray = new Map[] {};
    ResponseEntity<Map[]> realmsResponse = new ResponseEntity<>(realmsArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map[].class)))
        .thenReturn(realmsResponse);

    // Mock realm creation to throw exception
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"), any(HttpEntity.class), eq(Void.class)))
        .thenThrow(new RestClientException("Connection refused"));

    boolean result = keycloakService.createRealm();

    assertFalse(result);
  }

  // ========== addRoleToRealm() tests ==========

  @Test
  void addRoleToRealm_shouldReturnTrue_whenRoleCreatedSuccessfully() {
    ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/roles"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenReturn(response);

    boolean result = keycloakService.addRolesToRealm(List.of("test-role"));

    assertTrue(result);
  }

  @Test
  void addRoleToRealm_shouldReturnFalse_whenCreationFails() {
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/roles"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenThrow(new RestClientException("Role already exists"));

    boolean result = keycloakService.addRolesToRealm(List.of("test-role"));

    assertFalse(result);
  }

  // ========== deleteRealm() tests ==========

  @Test
  void deleteRealm_shouldReturnTrue_whenDeletedSuccessfully() {
    ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.NO_CONTENT);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenReturn(response);

    boolean result = keycloakService.deleteRealm();

    assertTrue(result);
  }

  @Test
  void deleteRealm_shouldReturnFalse_whenDeletionFails() {
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenThrow(new RestClientException("Realm not found"));

    boolean result = keycloakService.deleteRealm();

    assertFalse(result);
  }

  // ========== createUserInRealm() tests ==========

  @Test
  void createUserInRealm_shouldSucceed_whenUserCreatedSuccessfully() {
    ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenReturn(response);

    assertDoesNotThrow(
        () -> keycloakService.createUserInRealm("password123", "test@example.com", "John", "Doe"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void createUserInRealm_shouldSendCorrectPayload() {
    ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
    ArgumentCaptor<HttpEntity<Map<String, Object>>> captor =
        ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            captor.capture(),
            eq(Void.class)))
        .thenReturn(response);

    assertDoesNotThrow(
        () -> keycloakService.createUserInRealm("password123", "test@example.com", "John", "Doe"));

    Map<String, Object> body = captor.getValue().getBody();
    assertNotNull(body);
    assertEquals("test@example.com", body.get("email"));
    assertEquals("John", body.get("firstName"));
    assertEquals("Doe", body.get("lastName"));
    assertTrue((Boolean) body.get("enabled"));
    assertEquals(List.of(TEST_CLIENT_ROLE), body.get("realmRoles"));
  }

  @Test
  void createUserInRealm_shouldThrowBadRequest_whenConflictOccurs() {
    HttpClientErrorException conflict =
        HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenThrow(conflict);

    assertThrows(
        BadRequestException.class,
        () -> keycloakService.createUserInRealm("password123", "test@example.com", "John", "Doe"));
  }

  @Test
  void createUserInRealm_shouldThrowInternalServerError_whenOtherErrorOccurs() {
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenThrow(new RestClientException("Connection issue"));

    assertThrows(
        InternalServerErrorException.class,
        () -> keycloakService.createUserInRealm("password123", "test@example.com", "John", "Doe"));
  }

  // ========== createAdminUserInRealm() tests ==========

  @Test
  void createAdminUserInRealm_shouldReturnTrue_whenAdminCreatedSuccessfully() {
    ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenReturn(response);

    boolean result =
        keycloakService.createAdminUserInRealm("admin123", "admin@example.com", "Admin", "User");

    assertTrue(result);
  }

  @Test
  @SuppressWarnings("unchecked")
  void createAdminUserInRealm_shouldAssignAdminRole() {
    ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
    ArgumentCaptor<HttpEntity<Map<String, Object>>> captor =
        ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            captor.capture(),
            eq(Void.class)))
        .thenReturn(response);

    keycloakService.createAdminUserInRealm("admin123", "admin@example.com", "Admin", "User");

    Map<String, Object> body = captor.getValue().getBody();
    assertNotNull(body);
    assertEquals(List.of(TEST_CLIENT_ROLE, TEST_ADMIN_ROLE), body.get("realmRoles"));
  }

  @Test
  void createAdminUserInRealm_shouldThrowBadRequest_whenAdminWithSameEmailExists() {
    HttpClientErrorException conflict =
        HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null);
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenThrow(conflict);

    assertThrows(
        BadRequestException.class,
        () ->
            keycloakService.createAdminUserInRealm(
                "admin123", "testadmin@studyconnect.test", "Test", "Admin"));
  }

  @Test
  void createAdminUserInRealm_shouldThrowInternalServerError_whenOtherErrorOccurs() {
    when(restTemplate.postForEntity(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            any(HttpEntity.class),
            eq(Void.class)))
        .thenThrow(new RestClientException("Connection issue"));

    assertThrows(
        InternalServerErrorException.class,
        () ->
            keycloakService.createAdminUserInRealm(
                "admin123", "admin@example.com", "Admin", "User"));
  }

  // ========== retrieveUserByUUID() tests ==========

  @Test
  void retrieveUserByUUID_shouldReturnUser_whenFound() {
    String userId = "user-uuid-123";
    KeycloakUserResponseDTO expectedUser =
        new KeycloakUserResponseDTO(
            userId,
            System.currentTimeMillis(),
            "testuser",
            true,
            true,
            "John",
            "Doe",
            "test@example.com",
            null,
            null,
            null);
    ResponseEntity<KeycloakUserResponseDTO> response =
        new ResponseEntity<>(expectedUser, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users/" + userId),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO.class)))
        .thenReturn(response);

    KeycloakUserResponseDTO result = keycloakService.retrieveUserByUUID(userId);

    assertNotNull(result);
    assertEquals(userId, result.getKeycloakUUID());
    assertEquals("John", result.getFirstName());
  }

  @Test
  void retrieveUserByUUID_shouldReturnNull_whenNotFound() {
    String userId = "non-existent-uuid";
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users/" + userId),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO.class)))
        .thenThrow(new RestClientException("User not found"));

    KeycloakUserResponseDTO result = keycloakService.retrieveUserByUUID(userId);

    assertNull(result);
  }

  // ========== retrieveUserByEmail() tests ==========

  @Test
  void retrieveUserByEmail_shouldReturnUser_whenFound() {
    String email = "test@example.com";
    KeycloakUserResponseDTO expectedUser =
        new KeycloakUserResponseDTO(
            "user-uuid",
            System.currentTimeMillis(),
            "testuser",
            true,
            true,
            "John",
            "Doe",
            email,
            null,
            null,
            null);
    KeycloakUserResponseDTO[] usersArray = new KeycloakUserResponseDTO[] {expectedUser};
    ResponseEntity<KeycloakUserResponseDTO[]> response =
        new ResponseEntity<>(usersArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users?email=" + email),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO[].class)))
        .thenReturn(response);

    KeycloakUserResponseDTO result = keycloakService.retrieveUserByEmail(email);

    assertNotNull(result);
    assertEquals(email, result.getEmail());
  }

  @Test
  void retrieveUserByEmail_shouldReturnNull_whenNotFound() {
    String email = "nonexistent@example.com";
    KeycloakUserResponseDTO[] emptyArray = new KeycloakUserResponseDTO[] {};
    ResponseEntity<KeycloakUserResponseDTO[]> response =
        new ResponseEntity<>(emptyArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users?email=" + email),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO[].class)))
        .thenReturn(response);

    KeycloakUserResponseDTO result = keycloakService.retrieveUserByEmail(email);

    assertNull(result);
  }

  @Test
  void retrieveUserByEmail_shouldReturnNull_whenExceptionOccurs() {
    String email = "test@example.com";
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users?email=" + email),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO[].class)))
        .thenThrow(new RestClientException("Connection error"));

    KeycloakUserResponseDTO result = keycloakService.retrieveUserByEmail(email);

    assertNull(result);
  }

  // ========== retrieveAllUsersInRealm() tests ==========

  @Test
  void retrieveAllUsersInRealm_shouldReturnUsers_whenFound() {
    KeycloakUserResponseDTO user1 =
        new KeycloakUserResponseDTO(
            "uuid-1",
            System.currentTimeMillis(),
            "user1",
            true,
            true,
            "John",
            "Doe",
            "john@example.com",
            null,
            null,
            null);
    KeycloakUserResponseDTO user2 =
        new KeycloakUserResponseDTO(
            "uuid-2",
            System.currentTimeMillis(),
            "user2",
            true,
            true,
            "Jane",
            "Doe",
            "jane@example.com",
            null,
            null,
            null);
    KeycloakUserResponseDTO[] usersArray = new KeycloakUserResponseDTO[] {user1, user2};
    ResponseEntity<KeycloakUserResponseDTO[]> response =
        new ResponseEntity<>(usersArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO[].class)))
        .thenReturn(response);

    List<KeycloakUserResponseDTO> result = keycloakService.retrieveAllUsersInRealm();

    assertEquals(2, result.size());
    assertEquals("John", result.get(0).getFirstName());
    assertEquals("Jane", result.get(1).getFirstName());
  }

  @Test
  void retrieveAllUsersInRealm_shouldReturnEmptyList_whenNoUsersFound() {
    ResponseEntity<KeycloakUserResponseDTO[]> response = new ResponseEntity<>(null, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO[].class)))
        .thenReturn(response);

    List<KeycloakUserResponseDTO> result = keycloakService.retrieveAllUsersInRealm();

    assertTrue(result.isEmpty());
  }

  @Test
  void retrieveAllUsersInRealm_shouldReturnEmptyList_whenExceptionOccurs() {
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms/" + TEST_REALM + "/users"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(KeycloakUserResponseDTO[].class)))
        .thenThrow(new RestClientException("Connection error"));

    List<KeycloakUserResponseDTO> result = keycloakService.retrieveAllUsersInRealm();

    assertTrue(result.isEmpty());
  }

  // ========== getRealmName() tests ==========

  @Test
  void getRealmName_shouldReturnConfiguredRealmName() {
    String result = keycloakService.getRealmName();

    assertEquals(TEST_REALM, result);
  }

  // ========== getAllRealms() tests ==========

  @Test
  @SuppressWarnings("unchecked")
  void getAllRealms_shouldReturnRealmNames_whenRealmsExist() {
    Map<String, Object>[] realmsArray =
        new Map[] {Map.of("realm", "realm1"), Map.of("realm", "realm2"), Map.of("realm", "realm3")};
    ResponseEntity<Map[]> response = new ResponseEntity<>(realmsArray, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map[].class)))
        .thenReturn(response);

    List<String> result = keycloakService.getAllRealms();

    assertEquals(3, result.size());
    assertTrue(result.contains("realm1"));
    assertTrue(result.contains("realm2"));
    assertTrue(result.contains("realm3"));
  }

  @Test
  void getAllRealms_shouldReturnEmptyList_whenNoRealms() {
    ResponseEntity<Map[]> response = new ResponseEntity<>(null, HttpStatus.OK);
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map[].class)))
        .thenReturn(response);

    List<String> result = keycloakService.getAllRealms();

    assertTrue(result.isEmpty());
  }

  @Test
  void getAllRealms_shouldReturnEmptyList_whenExceptionOccurs() {
    when(restTemplate.exchange(
            eq(TEST_KEYCLOAK_URL + "/admin/realms"),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(Map[].class)))
        .thenThrow(new RestClientException("Connection error"));

    List<String> result = keycloakService.getAllRealms();

    assertTrue(result.isEmpty());
  }
}
