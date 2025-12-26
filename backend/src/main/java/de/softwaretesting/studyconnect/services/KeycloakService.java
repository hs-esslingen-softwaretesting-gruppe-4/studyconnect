package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Service for Keycloak-related operations. */
@RequiredArgsConstructor
@Service
public class KeycloakService {

  private final KeycloakAdminTokenService keycloakAdminTokenService;
  private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakService.class);
  private static final String KEYCLOAK_REALM_PATH = "/admin/realms/";
  private final RestTemplate restTemplate;

  @Value("${KEYCLOAK_AUTH_SERVER_URL}")
  private String keycloakServerUrl;

  @Value("${keycloak.realm}")
  private String realmName;

  @Value("${keycloak.default-client-role}")
  private String defaultClientRole;

  @Value("${keycloak.default-admin-role}")
  private String defaultAdminRole;

  @Value("${KEYCLOAK_CLIENT_ID}")
  private String clientId;

  @Value("${KEYCLOAK_DEVELOPMENT_CLIENT_ID}")
  private String developmentClientId;

  @Value("${ALLOWED_ORIGIN}")
  private String allowedOrigin;

  @Value("${KEYCLOAK_CLIENT_SECRET}")
  private String clientSecret;

  /**
   * Returns a valid access token from the token service.
   *
   * @return the current valid access token
   */
  private String getAccessToken() {
    return keycloakAdminTokenService.getAccessToken();
  }

  /**
   * Creates a new realm in Keycloak if it does not already exist.
   *
   * @return true if the realm was created successfully or already exists, false otherwise
   */
  public boolean createRealm() {
    String url = keycloakServerUrl + "/admin/realms";

    // First, check if the realm already exists
    List<String> existingRealms = getAllRealms();
    if (existingRealms.contains(realmName)) {
      LOGGER.info("Realm '{}' already exists in Keycloak", realmName);
      return true; // Realm already exists
    }

    // If the realm doesn't exist, create it
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());

    Map<String, Object> body =
        Map.of(
            "realm", realmName,
            "enabled", true,
            "registrationEmailAsUsername", true, // use email as identifier instead of username
            "duplicateEmailsAllowed", false // prevent duplicate emails
            );

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
      if (response.getStatusCode().is2xxSuccessful()) {
        LOGGER.info("Realm '{}' created successfully in Keycloak", realmName);
        return true;
      }
      return false;

    } catch (Exception e) {
      LOGGER.error("Error creating realm: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Adds a role to the configured realm in Keycloak.
   *
   * @param roleName the name of the role to add
   * @return true if the role was added successfully, false otherwise
   */
  public boolean addRolesToRealm(List<String> roles) {
    String roleUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/roles";

    LOGGER.info("Roles to add: {}", roles);
    List<String> existingRoles = getAllRoles();
    try {
      for (String roleName : roles) {
        if (existingRoles.contains(roleName)) {
          LOGGER.info("Role '{}' already exists in realm '{}'", roleName, realmName);
          continue; // Role already exists
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());

        Map<String, Object> body = Map.of("name", roleName);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(roleUrl, request, Void.class);
        LOGGER.info("Role '{}' added successfully to realm '{}'", roleName, realmName);
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Error adding roles to realm");
      return false;
    }
  }

  /**
   * Creates the configured clients in Keycloak if they do not already exist.
   *
   * @return true if the clients were created successfully or already exist, false otherwise
   */
  public boolean createClients() {

    // Check if client already exists
    List<String> existingClients = getAllClients();
    if (!existingClients.contains(clientId) && !existingClients.contains(developmentClientId)) {
      LOGGER.info(
          "Clients '{}' or '{}' do not exist in realm '{}'",
          clientId,
          developmentClientId,
          realmName);
      return createClientsInRealm(List.of(clientId, developmentClientId));
    } else if (!existingClients.contains(clientId)) {
      LOGGER.info("Client '{}' does not exist in realm '{}'", clientId, realmName);
      return createClientsInRealm(List.of(clientId));
    } else if (!existingClients.contains(developmentClientId)) {
      LOGGER.info("Client '{}' does not exist in realm '{}'", developmentClientId, realmName);
      return createClientsInRealm(List.of(developmentClientId));
    } else {
      LOGGER.info(
          "Clients '{}' and '{}' already exist in realm '{}'",
          clientId,
          developmentClientId,
          realmName);
      return true;
    }
  }

  /**
   * Deletes the configured realm in Keycloak.
   *
   * @return true if the realm was deleted successfully, false otherwise
   */
  public boolean deleteRealm() {
    String deleteUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<Void> response =
          restTemplate.exchange(
              deleteUrl, org.springframework.http.HttpMethod.DELETE, request, Void.class);
      LOGGER.info("Realm '{}' deleted successfully from Keycloak", realmName);
      return response.getStatusCode().is2xxSuccessful();

    } catch (Exception e) {
      LOGGER.error("Error deleting realm: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Creates a user in the configured realm.
   *
   * @param password the password for the new user
   * @param email the email of the new user
   * @param surname the surname of the new user
   * @param lastname the last name of the new user
   * @return true if the user was created successfully, false otherwise
   */
  public void createUserInRealm(String password, String email, String surname, String lastname) {

    String userUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());

    Map<String, Object> credentials =
        Map.of("type", "password", "value", password, "temporary", false);

    Map<String, Object> body =
        Map.of(
            "enabled",
            true,
            "email",
            email,
            "firstName",
            surname,
            "lastName",
            lastname,
            "realmRoles",
            List.of(defaultClientRole),
            "credentials",
            List.of(credentials));

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Void> response = restTemplate.postForEntity(userUrl, request, Void.class);
      LOGGER.info(
          "User '{}' '{}' created successfully in realm '{}', status code: {}",
          surname,
          lastname,
          realmName,
          response.getStatusCode());
      boolean isSuccessful = response.getStatusCode().is2xxSuccessful();
      LOGGER.debug("Status code is2xxSuccessful: {}", isSuccessful);

    } catch (HttpClientErrorException e) {
      // Translate 409 conflicts into a BadRequestException so callers can return 4xx
      if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
        LOGGER.warn(
            "Conflict creating user with email {} in realm {}: {}",
            email,
            realmName,
            e.getResponseBodyAsString());
        throw new BadRequestException("User with email " + email + " already exists");
      }

      LOGGER.error("HTTP error creating user in realm: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Error during user creation in Keycloak: " + e.getMessage());

    } catch (Exception e) {
      LOGGER.error("Error creating user in realm: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Unknown error during user creation in Keycloak: " + e.getMessage());
    }
  }

  /**
   * Creates an admin user in the configured realm.
   *
   * @param password the password for the new admin user
   * @param email the email of the new admin user
   * @param surname the surname of the new admin user
   * @param lastname the last name of the new admin user
   * @return true if the admin user was created successfully, false otherwise
   */
  public boolean createAdminUserInRealm(
      String password, String email, String surname, String lastname) {
    String userUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());

    Map<String, Object> credentials =
        Map.of("type", "password", "value", password, "temporary", false);

    Map<String, Object> body =
        Map.of(
            "enabled",
            true,
            "email",
            email,
            "firstName",
            surname,
            "lastName",
            lastname,
            "realmRoles",
            List.of(defaultClientRole, defaultAdminRole),
            "credentials",
            List.of(credentials));

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Void> response = restTemplate.postForEntity(userUrl, request, Void.class);
      LOGGER.info(
          "Admin user '{}' '{}' created successfully in realm '{}'", surname, lastname, realmName);
      return response.getStatusCode().is2xxSuccessful();

    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
        LOGGER.warn(
            "Conflict creating admin user with email {} in realm {}: {}",
            email,
            realmName,
            e.getResponseBodyAsString());
        throw new BadRequestException("Admin user with email " + email + " already exists");
      }
      LOGGER.error("HTTP error creating admin user in realm: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Error during admin user creation in Keycloak: " + e.getMessage());
    } catch (Exception e) {
      LOGGER.error("Error creating admin user in realm: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Unknown error during admin user creation in Keycloak: " + e.getMessage());
    }
  }

  /**
   * Retrieves a user by UUID from the configured realm.
   *
   * @param userId the UUID of the user to retrieve
   * @return the user DTO, or null if not found
   */
  public KeycloakUserResponseDTO retrieveUserByUUID(String userId) {
    String userUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/users/" + userId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<KeycloakUserResponseDTO> response =
          restTemplate.exchange(
              userUrl,
              org.springframework.http.HttpMethod.GET,
              request,
              KeycloakUserResponseDTO.class);
      LOGGER.info("User '{}' retrieved successfully from realm '{}'", userId, realmName);
      return response.getBody();

    } catch (Exception e) {
      LOGGER.error("Error retrieving user by UUID: {}", e.getMessage());
      return null;
    }
  }

  public KeycloakUserResponseDTO retrieveUserByEmail(String email) {

    String userUrl =
        UriComponentsBuilder.fromUriString(keycloakServerUrl)
            .pathSegment("admin", "realms", realmName, "users")
            .queryParam("email", email)
            .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<KeycloakUserResponseDTO[]> response =
          restTemplate.exchange(
              userUrl,
              org.springframework.http.HttpMethod.GET,
              request,
              KeycloakUserResponseDTO[].class);
      LOGGER.info("User with email '{}' retrieved successfully from realm '{}'", email, realmName);
      KeycloakUserResponseDTO[] users = response.getBody();
      if (users != null && users.length > 0) {
        return users[0]; // Return the first matching user
      } else {
        return null; // No user found
      }

    } catch (HttpClientErrorException e) {
      LOGGER.error("HTTP error retrieving user by email {}: {}", email, e.getMessage());
      return null;
    } catch (Exception e) {
      LOGGER.error("Error retrieving user by email {}: {}", email, e.getMessage());
      return null;
    }
  }

  /**
   * Retrieves all users from the configured realm.
   *
   * @return list of user DTOs, or empty list if none found
   */
  public List<KeycloakUserResponseDTO> retrieveAllUsersInRealm() {
    String usersUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/users";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<KeycloakUserResponseDTO[]> response =
          restTemplate.exchange(
              usersUrl,
              org.springframework.http.HttpMethod.GET,
              request,
              KeycloakUserResponseDTO[].class);
      return response.getBody() != null
          ? Arrays.asList(response.getBody())
          : Collections.emptyList();

    } catch (Exception e) {
      LOGGER.error("Error retrieving all users in realm: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Returns the configured realm name.
   *
   * @return the realm name
   */
  public String getRealmName() {
    return realmName;
  }

  /**
   * Retrieves all realms from Keycloak.
   *
   * @return list of realm names, or empty list if none found
   */
  public List<String> getAllRealms() {
    String realmsUrl = keycloakServerUrl + "/admin/realms";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map[]> response =
          restTemplate.exchange(
              realmsUrl, org.springframework.http.HttpMethod.GET, request, Map[].class);
      LOGGER.info("All realms retrieved successfully");
      if (response.getBody() != null) {
        return Arrays.stream(response.getBody()).map(realm -> (String) realm.get("realm")).toList();
      } else {
        return Collections.emptyList();
      }

    } catch (Exception e) {
      LOGGER.error("Error retrieving all realms: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Retrieves all clients from the configured realm.
   *
   * @return list of client IDs, or empty list if none found
   */
  public List<String> getAllClients() {
    String clientsUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/clients";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map[]> response =
          restTemplate.exchange(
              clientsUrl, org.springframework.http.HttpMethod.GET, request, Map[].class);
      if (response.getBody() != null) {

        return Arrays.stream(response.getBody())
            .map(client -> (String) client.get("clientId"))
            .toList();
      } else {
        return Collections.emptyList();
      }

    } catch (Exception e) {
      LOGGER.error("Error retrieving all clients: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  private List<String> getAllRoles() {
    String rolesUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/roles";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map[]> response =
          restTemplate.exchange(
              rolesUrl, org.springframework.http.HttpMethod.GET, request, Map[].class);
      if (response.getBody() != null) {

        return Arrays.stream(response.getBody()).map(role -> (String) role.get("name")).toList();
      } else {
        return Collections.emptyList();
      }

    } catch (Exception e) {
      LOGGER.error("Error retrieving all roles: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Creates clients in the configured realm.
   *
   * @param clientIds the list of client IDs to create
   * @return true if the clients were created successfully, false otherwise
   */
  private boolean createClientsInRealm(List<String> clientIds) {

    String clientUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/clients";
    try {
      for (String id : clientIds) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());

        Map<String, Object> attributes =
            Map.ofEntries(
                Map.entry("pkce.code.challenge.method", "S256"),
                Map.entry("pkce.supported", "true"),
                Map.entry("post.logout.redirect.uris", allowedOrigin + "/*"),
                Map.entry("frontchannel.logout.session.required", "true"),
                Map.entry("frontchannel.logout.url", allowedOrigin + "/logout"));

        Map<String, Object> body =
            Map.ofEntries(
                Map.entry("clientId", id),
                Map.entry("enabled", true),
                Map.entry("publicClient", true),
                Map.entry("description", "Client for Studyconnect frontend application"),
                Map.entry("redirectUris", (Object) List.of(allowedOrigin, allowedOrigin + "/*")),
                Map.entry("standardFlowEnabled", true),
                Map.entry("directAccessGrantsEnabled", false),
                Map.entry("implicitFlowEnabled", false),
                Map.entry("serviceAccountsEnabled", false),
                Map.entry("authorizationServicesEnabled", false),
                Map.entry("attributes", attributes),
                Map.entry(
                    "defaultClientScopes", (Object) List.of("openid", "profile", "email", "roles")),
                Map.entry("webOrigins", (Object) List.of(allowedOrigin)),
                Map.entry("protocol", "openid-connect"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(clientUrl, request, Void.class);
        LOGGER.info("Client '{}' created successfully in realm '{}'", id, realmName);
      }
      return true;
    } catch (Exception e) {
      LOGGER.error("Error creating clients in realm: {}", e.getMessage());
      return false;
    }
  }
}
