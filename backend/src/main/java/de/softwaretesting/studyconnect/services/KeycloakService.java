package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/** Service for Keycloak-related operations. */
@RequiredArgsConstructor
@Service
public class KeycloakService {

  private final KeycloakAdminTokenService keycloakAdminTokenService;
  private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakService.class);
  private final RestTemplate restTemplate;

  @Value("${KEYCLOAK_AUTH_SERVER_URL}")
  private String keycloakServerUrl;

  @Value("${keycloak.realm}")
  private String realmName;

  @Value("${keycloak.default-client-role}")
  private String defaultClientRole;

  @Value("${keycloak.default-admin-role}")
  private String defaultAdminRole;

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
        // Create default roles
        addRoleToRealm(defaultClientRole);
        addRoleToRealm(defaultAdminRole);
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
  public boolean addRoleToRealm(String roleName) {
    String roleUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/roles";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(getAccessToken());

    Map<String, Object> body = Map.of("name", roleName);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Void> response = restTemplate.postForEntity(roleUrl, request, Void.class);
      LOGGER.info("Role '{}' added successfully to realm '{}'", roleName, realmName);
      return response.getStatusCode().is2xxSuccessful();

    } catch (Exception e) {
      LOGGER.error("Error adding role to realm: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Deletes the configured realm in Keycloak.
   *
   * @return true if the realm was deleted successfully, false otherwise
   */
  public boolean deleteRealm() {
    String deleteUrl = keycloakServerUrl + "/admin/realms/" + realmName;

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
  public boolean createUserInRealm(String password, String email, String surname, String lastname) {

    String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users";

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
          "User '{}' '{}' created successfully in realm '{}'", surname, lastname, realmName);
      return response.getStatusCode().is2xxSuccessful();

    } catch (Exception e) {
      LOGGER.error("Error creating user in realm: {}", e.getMessage());
      return false;
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
    String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users";

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

    } catch (Exception e) {
      LOGGER.error("Error creating admin user in realm: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Retrieves a user by UUID from the configured realm.
   *
   * @param userId the UUID of the user to retrieve
   * @return the user DTO, or null if not found
   */
  public KeycloakUserResponseDTO retrieveUserByUUID(String userId) {
    String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users/" + userId;

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

    } catch (Exception e) {
      LOGGER.error("Error retrieving user by email: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Retrieves all users from the configured realm.
   *
   * @return list of user DTOs, or empty list if none found
   */
  public List<KeycloakUserResponseDTO> retrieveAllUsersInRealm() {
    String usersUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users";

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
      LOGGER.info("All users retrieved successfully from realm '{}'", realmName);
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
}
