package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.response.KeycloakTokenResponseDTO;
import de.softwaretesting.studyconnect.exceptions.KeycloakTokenFetchException;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Service for managing Keycloak admin tokens. Handles token retrieval, storage, and automatic
 * refresh.
 */
@Service
public class KeycloakAdminTokenService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAdminTokenService.class);

  /**
   * Buffer time in seconds before token expiry to trigger refresh. Token will be refreshed 60
   * seconds before it actually expires.
   */
  private static final int REFRESH_BUFFER_SECONDS = 60;

  @Value("${KEYCLOAK_AUTH_SERVER_URL}")
  private String keycloakServerUrl;

  @Value("${KEYCLOAK_ADMIN}")
  private String adminUsername;

  @Value("${KEYCLOAK_ADMIN_PASSWORD}")
  private String adminPassword;

  private final RestTemplate restTemplate;
  private final ReentrantLock tokenLock = new ReentrantLock();

  private String accessToken;
  private String refreshToken;
  private Instant tokenExpiryTime;
  private Instant refreshTokenExpiryTime;

  public KeycloakAdminTokenService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  /** Initializes the service by fetching an initial token on application startup. */
  @PostConstruct
  public void init() {
    if (adminUsername == null
        || adminUsername.isBlank()
        || adminPassword == null
        || adminPassword.isBlank()) {
      LOGGER.warn(
          "Keycloak admin credentials not configured. Token service will not be initialized. Please set KEYCLOAK_ADMIN and KEYCLOAK_ADMIN_PASSWORD environment variables.");
      return;
    }

    try {
      fetchNewToken();
      LOGGER.info("Keycloak admin token successfully initialized");
    } catch (Exception e) {
      LOGGER.error("Failed to initialize Keycloak admin token: {}", e.getMessage());
    }
  }

  /**
   * Returns a valid access token. If the current token is expired or about to expire, it will be
   * refreshed automatically.
   *
   * @return the current valid access token, or null if unavailable
   */
  public String getAccessToken() {
    tokenLock.lock();
    try {
      if (accessToken == null) {
        fetchNewToken();
      } else if (isTokenExpired()) {
        refreshAccessToken();
      }
      return accessToken;
    } catch (Exception e) {
      LOGGER.error("Failed to get access token: {}", e.getMessage());
      return null;
    } finally {
      tokenLock.unlock();
    }
  }

  /**
   * Checks if the current access token is expired or about to expire.
   *
   * @return true if the token needs to be refreshed
   */
  private boolean isTokenExpired() {
    if (tokenExpiryTime == null) {
      return true;
    }
    return Instant.now().isAfter(tokenExpiryTime.minusSeconds(REFRESH_BUFFER_SECONDS));
  }

  /**
   * Checks if the refresh token is expired.
   *
   * @return true if the refresh token is expired
   */
  private boolean isRefreshTokenExpired() {
    if (refreshTokenExpiryTime == null) {
      return true;
    }
    return Instant.now().isAfter(refreshTokenExpiryTime.minusSeconds(REFRESH_BUFFER_SECONDS));
  }

  /** Fetches a new access token using the admin credentials. */
  private void fetchNewToken() {
    LOGGER.info("Fetching new Keycloak admin token");

    String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", "admin-cli");
    body.add("username", adminUsername);
    body.add("password", adminPassword);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<KeycloakTokenResponseDTO> response =
          restTemplate.postForEntity(tokenUrl, request, KeycloakTokenResponseDTO.class);

      if (response.getBody() != null) {
        updateTokens(response.getBody());
        LOGGER.info("Successfully fetched new Keycloak admin token");
      }
    } catch (Exception e) {
      LOGGER.error("Failed to fetch Keycloak admin token: {}", e.getMessage());
      throw new KeycloakTokenFetchException("Failed to fetch Keycloak admin token");
    }
  }

  /**
   * Refreshes the access token using the refresh token. If the refresh token is expired, fetches a
   * completely new token.
   */
  private void refreshAccessToken() {
    if (isRefreshTokenExpired() || refreshToken == null) {
      LOGGER.debug("Refresh token expired, fetching new token");
      fetchNewToken();
      return;
    }

    LOGGER.debug("Refreshing Keycloak admin token");

    String tokenUrl = keycloakServerUrl + "/realms/master/protocol/openid-connect/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "refresh_token");
    body.add("client_id", "admin-cli");
    body.add("refresh_token", refreshToken);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<KeycloakTokenResponseDTO> response =
          restTemplate.postForEntity(tokenUrl, request, KeycloakTokenResponseDTO.class);

      if (response.getBody() != null) {
        updateTokens(response.getBody());
        LOGGER.info("Successfully refreshed Keycloak admin token");
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to refresh token, attempting to fetch new token: {}", e.getMessage());
      fetchNewToken();
    }
  }

  /**
   * Updates the stored tokens from the response.
   *
   * @param tokenResponse the response from Keycloak
   */
  private void updateTokens(KeycloakTokenResponseDTO tokenResponse) {
    this.accessToken = tokenResponse.getAccessToken();
    this.refreshToken = tokenResponse.getRefreshToken();
    this.tokenExpiryTime = Instant.now().plusSeconds(tokenResponse.getExpiresIn());

    if (tokenResponse.getRefreshExpiresIn() != null && tokenResponse.getRefreshExpiresIn() > 0) {
      this.refreshTokenExpiryTime = Instant.now().plusSeconds(tokenResponse.getRefreshExpiresIn());
    } else {
      // Default refresh token expiry to 30 minutes if not provided
      this.refreshTokenExpiryTime = Instant.now().plusSeconds(1800);
    }
  }

  /**
   * Scheduled task to proactively refresh the token before it expires. Runs every 30 seconds to
   * check if the token needs refreshing.
   */
  @Scheduled(fixedRate = 30000)
  public void scheduleTokenRefresh() {
    if (accessToken == null) {
      return; // Service not initialized
    }

    tokenLock.lock();
    try {
      if (isTokenExpired()) {
        LOGGER.debug("Scheduled token refresh triggered");
        refreshAccessToken();
      }
    } catch (Exception e) {
      LOGGER.error("Scheduled token refresh failed: {}", e.getMessage());
    } finally {
      tokenLock.unlock();
    }
  }

  /** Invalidates the current tokens, forcing a new token to be fetched on next access. */
  public void invalidateTokens() {
    tokenLock.lock();
    try {
      this.accessToken = null;
      this.refreshToken = null;
      this.tokenExpiryTime = null;
      this.refreshTokenExpiryTime = null;
      LOGGER.info("Keycloak admin tokens invalidated");
    } finally {
      tokenLock.unlock();
    }
  }
}
