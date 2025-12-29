package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import jakarta.annotation.PostConstruct;
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
import org.springframework.web.util.UriComponents;
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

  @Value("${keycloak.client-id}")
  private String clientId;

  @Value("${keycloak.development.client-id}")
  private String developmentClientId;

  @Value("${ALLOWED_ORIGIN}")
  private String allowedOrigin;

  @PostConstruct
  void normalizeConfig() {
    realmName = normalize("keycloak.realm", realmName);
    defaultClientRole = normalize("keycloak.default-client-role", defaultClientRole);
    defaultAdminRole = normalize("keycloak.default-admin-role", defaultAdminRole);
    clientId = normalize("keycloak.client-id", clientId);
    developmentClientId = normalize("keycloak.development.client-id", developmentClientId);
    allowedOrigin = normalize("allowed.origin", allowedOrigin);
    keycloakServerUrl = normalize("KEYCLOAK_AUTH_SERVER_URL", keycloakServerUrl);
  }

  private String normalize(String name, String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (!trimmed.equals(value)) {
      LOGGER.warn("Config value '{}' has surrounding whitespace; trimming.", name);
    }
    return trimmed;
  }

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

    List<String> normalizedRoles =
        roles == null
            ? List.of()
            : roles.stream()
                .filter(r -> r != null && !r.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

    LOGGER.info("Roles to add: {}", normalizedRoles);
    List<String> existingRoles = getAllRoles();
    try {
      for (String roleName : normalizedRoles) {
        if (existingRoles.contains(roleName)) {
          LOGGER.info("Role '{}' already exists in realm '{}'", roleName, realmName);
          continue; // Role already exists
        }

        boolean whitespaceVariantExists =
            existingRoles.stream()
                .filter(r -> r != null && !r.isBlank())
                .anyMatch(r -> r.trim().equals(roleName));
        if (whitespaceVariantExists) {
          LOGGER.warn(
              "Role '{}' exists with surrounding whitespace in realm '{}'; creating normalized role name.",
              roleName,
              realmName);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());

        Map<String, Object> body =
            Map.of("name", roleName, "description", "Realm role for Studyconnect application");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
          restTemplate.postForEntity(roleUrl, request, Void.class);
          LOGGER.info("Role '{}' added successfully to realm '{}'", roleName, realmName);
        } catch (HttpClientErrorException e) {
          if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
            LOGGER.info(
                "Role '{}' already exists in realm '{}' (409 CONFLICT)", roleName, realmName);
            continue;
          }
          throw e;
        }
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

      String createdUserId = extractUserIdFromLocation(response);
      LOGGER.info("Created user ID from Location: " + createdUserId);
      if (createdUserId == null) {
        // Fallback to lookup by email (Keycloak sometimes omits Location in some proxy setups)
        KeycloakUserResponseDTO createdUser = retrieveUserByEmail(email);
        createdUserId = createdUser != null ? createdUser.getKeycloakUUID() : null;
      }

      if (createdUserId == null) {
        throw new InternalServerErrorException(
            "User created in Keycloak but could not determine user id for role assignment");
      }

      assignRealmRolesToUser(createdUserId, List.of(defaultClientRole));

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
            "credentials",
            List.of(credentials));

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Void> response = restTemplate.postForEntity(userUrl, request, Void.class);
      LOGGER.info(
          "Admin user '{}' '{}' created successfully in realm '{}'", surname, lastname, realmName);

      String createdUserId = extractUserIdFromLocation(response);
      LOGGER.info("Created admin user ID from Location: " + createdUserId);
      if (createdUserId == null) {
        KeycloakUserResponseDTO createdUser = retrieveUserByEmail(email);
        createdUserId = createdUser != null ? createdUser.getKeycloakUUID() : null;
      }
      if (createdUserId != null) {
        assignRealmRolesToUser(createdUserId, List.of(defaultClientRole, defaultAdminRole));
      } else {
        LOGGER.warn(
            "Admin user created but could not determine user id for role assignment (email={})",
            email);
      }

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
   * Extracts the user ID from the Location header of a Keycloak create user response.
   *
   * @param response the response entity from the create user request
   * @return the extracted user ID, or null if it cannot be determined
   */
  private String extractUserIdFromLocation(ResponseEntity<Void> response) {
    if (response == null || response.getHeaders() == null) {
      return null;
    }
    var location = response.getHeaders().getLocation();
    if (location == null) {
      return null;
    }
    try {
      UriComponents components = UriComponentsBuilder.fromUri(location).build();
      List<String> segments = components.getPathSegments();
      if (segments.isEmpty()) {
        return null;
      }
      return segments.get(segments.size() - 1);
    } catch (Exception e) {
      LOGGER.warn("Failed to parse Keycloak Location header '{}': {}", location, e.getMessage());
      return null;
    }
  }

  private void assignRealmRolesToUser(String userId, List<String> roleNames) {
    if (roleNames == null || roleNames.isEmpty()) {
      return;
    }

    List<String> normalizedRoleNames =
        roleNames.stream()
            .filter(r -> r != null && !r.isBlank())
            .map(String::trim)
            .distinct()
            .toList();
    if (normalizedRoleNames.isEmpty()) {
      return;
    }

    String url =
        keycloakServerUrl
            + KEYCLOAK_REALM_PATH
            + realmName
            + "/users/"
            + userId
            + "/role-mappings/realm";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());

    List<Map<String, Object>> availableRoles = getAllRoleRepresentations();
    List<Map<String, Object>> roleRepresentations =
        normalizedRoleNames.stream()
            .map(roleName -> findRoleRepresentation(availableRoles, roleName))
            .toList();

    HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(roleRepresentations, headers);

    try {
      restTemplate.postForEntity(url, request, Void.class);
      LOGGER.info(
          "Assigned realm roles {} to user {} in realm {}", normalizedRoleNames, userId, realmName);
    } catch (Exception e) {
      LOGGER.error(
          "Failed to assign roles {} to user {}: {}", normalizedRoleNames, userId, e.getMessage());
      throw new InternalServerErrorException(
          "Failed to assign Keycloak roles to user: " + e.getMessage());
    }
  }

  /**
   * Retrieves all role representations from the configured realm.
   *
   * @return list of role representations
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getAllRoleRepresentations() {
    String rolesUrl = keycloakServerUrl + KEYCLOAK_REALM_PATH + realmName + "/roles";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(getAccessToken());
    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<List> response =
          restTemplate.exchange(
              rolesUrl, org.springframework.http.HttpMethod.GET, request, List.class);
      List<Map<String, Object>> roles =
          (List<Map<String, Object>>) (response.getBody() != null ? response.getBody() : List.of());
      return roles;
    } catch (Exception e) {
      LOGGER.error("Error retrieving all roles for realm '{}': {}", realmName, e.getMessage());
      throw new InternalServerErrorException(
          "Failed to retrieve Keycloak roles: " + e.getMessage());
    }
  }

  private Map<String, Object> findRoleRepresentation(
      List<Map<String, Object>> availableRoles, String normalizedRoleName) {
    if (normalizedRoleName == null || normalizedRoleName.isBlank()) {
      throw new InternalServerErrorException("Role name is blank");
    }

    Map<String, Object> exact =
        availableRoles.stream()
            .filter(m -> normalizedRoleName.equals(m.get("name")))
            .findFirst()
            .orElse(null);

    Map<String, Object> trimmedMatch =
        exact != null
            ? exact
            : availableRoles.stream()
                .filter(
                    m -> {
                      Object name = m.get("name");
                      return name instanceof String
                          && ((String) name).trim().equals(normalizedRoleName);
                    })
                .findFirst()
                .orElse(null);

    if (trimmedMatch == null) {
      throw new InternalServerErrorException(
          "Role '" + normalizedRoleName + "' not found in Keycloak realm '" + realmName + "'");
    }

    Object id = trimmedMatch.get("id");
    Object name = trimmedMatch.get("name");
    if (!(id instanceof String) || !(name instanceof String)) {
      throw new InternalServerErrorException(
          "Invalid role representation for '" + normalizedRoleName + "' from Keycloak");
    }
    return Map.of("id", id, "name", name);
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
