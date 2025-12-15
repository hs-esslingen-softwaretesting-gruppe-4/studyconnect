package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import de.softwaretesting.studyconnect.dtos.response.KeycloakTokenResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminTokenServiceTest {

  @Mock private RestTemplate restTemplate;

  private KeycloakAdminTokenService tokenService;

  private static final String KEYCLOAK_URL = "https://keycloak.example.com";
  private static final String ADMIN_USERNAME = "admin";
  private static final String ADMIN_PASSWORD = "adminpassword";

  @BeforeEach
  void setUp() {
    tokenService = new KeycloakAdminTokenService(restTemplate);
    ReflectionTestUtils.setField(tokenService, "keycloakServerUrl", KEYCLOAK_URL);
    ReflectionTestUtils.setField(tokenService, "adminUsername", ADMIN_USERNAME);
    ReflectionTestUtils.setField(tokenService, "adminPassword", ADMIN_PASSWORD);
  }

  // ==================== init() Tests ====================

  @Test
  void init_shouldFetchToken_whenCredentialsAreConfigured() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();

    verify(restTemplate)
        .postForEntity(
            eq(KEYCLOAK_URL + "/realms/master/protocol/openid-connect/token"),
            any(HttpEntity.class),
            eq(KeycloakTokenResponseDTO.class));
  }

  @ParameterizedTest
  @CsvSource({"adminUsername, ''", "adminPassword, ''", "adminUsername, null"})
  void init_shouldNotFetchToken_whenCredentialsAreInvalid(String fieldName, String fieldValue) {
    Object value = "null".equals(fieldValue) ? null : fieldValue;
    ReflectionTestUtils.setField(tokenService, fieldName, value);

    tokenService.init();

    verify(restTemplate, never()).postForEntity(anyString(), any(), any());
  }

  @Test
  void init_shouldHandleException_whenTokenFetchFails() {
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenThrow(new RestClientException("Connection refused"));

    // Should not throw, just log error
    assertDoesNotThrow(() -> tokenService.init());
  }

  // ==================== getAccessToken() Tests ====================

  @Test
  void getAccessToken_shouldReturnToken_whenTokenIsValid() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();
    String token = tokenService.getAccessToken();

    assertEquals("test-access-token", token);
  }

  @Test
  void getAccessToken_shouldFetchNewToken_whenNoTokenExists() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    // Don't call init(), just call getAccessToken directly
    String token = tokenService.getAccessToken();

    assertEquals("test-access-token", token);
    verify(restTemplate)
        .postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class));
  }

  @Test
  void getAccessToken_shouldRefreshToken_whenTokenIsExpired() {
    // First, set up an expired token
    KeycloakTokenResponseDTO expiredTokenResponse =
        createTokenResponse(1L, 1800L); // 1 second expiry
    KeycloakTokenResponseDTO newTokenResponse =
        new KeycloakTokenResponseDTO(
            "new-access-token", "new-refresh-token", 300L, 1800L, "Bearer");

    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(expiredTokenResponse, HttpStatus.OK))
        .thenReturn(new ResponseEntity<>(newTokenResponse, HttpStatus.OK));

    tokenService.init();

    // Simulate token expiry by setting expiry time to past
    ReflectionTestUtils.setField(
        tokenService, "tokenExpiryTime", java.time.Instant.now().minusSeconds(100));

    String token = tokenService.getAccessToken();

    assertEquals("new-access-token", token);
    verify(restTemplate, times(2))
        .postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class));
  }

  @Test
  void getAccessToken_shouldReturnNull_whenFetchFails() {
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenThrow(new RestClientException("Connection refused"));

    String token = tokenService.getAccessToken();

    assertNull(token);
  }

  // ==================== invalidateTokens() Tests ====================

  @Test
  void invalidateTokens_shouldClearAllTokens() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();
    assertNotNull(tokenService.getAccessToken());

    tokenService.invalidateTokens();

    // After invalidation, accessToken field should be null
    assertNull(ReflectionTestUtils.getField(tokenService, "accessToken"));
    assertNull(ReflectionTestUtils.getField(tokenService, "refreshToken"));
    assertNull(ReflectionTestUtils.getField(tokenService, "tokenExpiryTime"));
    assertNull(ReflectionTestUtils.getField(tokenService, "refreshTokenExpiryTime"));
  }

  @Test
  void invalidateTokens_shouldForceFetchOnNextAccess() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();
    tokenService.invalidateTokens();

    // Reset mock to track new calls
    reset(restTemplate);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.getAccessToken();

    // Should fetch new token after invalidation
    verify(restTemplate)
        .postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class));
  }

  // ==================== scheduleTokenRefresh() Tests ====================

  @Test
  void scheduleTokenRefresh_shouldNotRefresh_whenTokenIsNull() {
    // Don't initialize - token is null
    tokenService.scheduleTokenRefresh();

    verify(restTemplate, never()).postForEntity(anyString(), any(), any());
  }

  @Test
  void scheduleTokenRefresh_shouldRefresh_whenTokenIsExpired() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();

    // Simulate token expiry
    ReflectionTestUtils.setField(
        tokenService, "tokenExpiryTime", java.time.Instant.now().minusSeconds(100));

    tokenService.scheduleTokenRefresh();

    // Should have fetched token twice: once in init, once in scheduled refresh
    verify(restTemplate, times(2))
        .postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class));
  }

  @Test
  void scheduleTokenRefresh_shouldNotRefresh_whenTokenIsStillValid() {
    KeycloakTokenResponseDTO tokenResponse = createTokenResponse(300L, 1800L);
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();
    tokenService.scheduleTokenRefresh();

    // Should only have fetched once in init
    verify(restTemplate, times(1))
        .postForEntity(anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class));
  }

  // ==================== Token Refresh with Refresh Token Tests ====================

  @Test
  void getAccessToken_shouldUseRefreshToken_whenAccessTokenExpiredButRefreshTokenValid() {
    KeycloakTokenResponseDTO initialToken = createTokenResponse(1L, 1800L);
    KeycloakTokenResponseDTO refreshedToken =
        new KeycloakTokenResponseDTO(
            "refreshed-access-token", "refreshed-refresh-token", 300L, 1800L, "Bearer");

    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(initialToken, HttpStatus.OK))
        .thenReturn(new ResponseEntity<>(refreshedToken, HttpStatus.OK));

    tokenService.init();

    // Expire access token but keep refresh token valid
    ReflectionTestUtils.setField(
        tokenService, "tokenExpiryTime", java.time.Instant.now().minusSeconds(100));

    String token = tokenService.getAccessToken();

    assertEquals("refreshed-access-token", token);
  }

  @Test
  void getAccessToken_shouldFetchNewToken_whenBothTokensExpired() {
    KeycloakTokenResponseDTO initialToken = createTokenResponse(1L, 1L);
    KeycloakTokenResponseDTO newToken =
        new KeycloakTokenResponseDTO("brand-new-token", "brand-new-refresh", 300L, 1800L, "Bearer");

    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(initialToken, HttpStatus.OK))
        .thenReturn(new ResponseEntity<>(newToken, HttpStatus.OK));

    tokenService.init();

    // Expire both tokens
    ReflectionTestUtils.setField(
        tokenService, "tokenExpiryTime", java.time.Instant.now().minusSeconds(100));
    ReflectionTestUtils.setField(
        tokenService, "refreshTokenExpiryTime", java.time.Instant.now().minusSeconds(100));

    String token = tokenService.getAccessToken();

    assertEquals("brand-new-token", token);
  }

  // ==================== Edge Cases ====================

  @Test
  void updateTokens_shouldSetDefaultRefreshExpiry_whenNotProvided() {
    KeycloakTokenResponseDTO tokenResponse =
        new KeycloakTokenResponseDTO(
            "test-access-token", "test-refresh-token", 300L, null, "Bearer");
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();

    java.time.Instant refreshExpiry =
        (java.time.Instant) ReflectionTestUtils.getField(tokenService, "refreshTokenExpiryTime");
    assertNotNull(refreshExpiry);
    // Should be approximately 30 minutes from now (default)
    assertTrue(refreshExpiry.isAfter(java.time.Instant.now().plusSeconds(1700)));
  }

  @Test
  void updateTokens_shouldSetDefaultRefreshExpiry_whenZeroProvided() {
    KeycloakTokenResponseDTO tokenResponse =
        new KeycloakTokenResponseDTO("test-access-token", "test-refresh-token", 300L, 0L, "Bearer");
    when(restTemplate.postForEntity(
            anyString(), any(HttpEntity.class), eq(KeycloakTokenResponseDTO.class)))
        .thenReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

    tokenService.init();

    java.time.Instant refreshExpiry =
        (java.time.Instant) ReflectionTestUtils.getField(tokenService, "refreshTokenExpiryTime");
    assertNotNull(refreshExpiry);
    assertTrue(refreshExpiry.isAfter(java.time.Instant.now().plusSeconds(1700)));
  }

  // ==================== Helper Methods ====================

  private KeycloakTokenResponseDTO createTokenResponse(Long expiresIn, Long refreshExpiresIn) {
    return new KeycloakTokenResponseDTO(
        "test-access-token", "test-refresh-token", expiresIn, refreshExpiresIn, "Bearer");
  }
}
