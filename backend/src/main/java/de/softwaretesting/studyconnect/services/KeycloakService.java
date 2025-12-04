package de.softwaretesting.studyconnect.services;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * Service for Keycloak-related operations.
 */
@RequiredArgsConstructor
@Service
public class KeycloakService {
    
    private final KeycloakAdminTokenService keycloakAdminTokenService;
    private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);
    private String accessToken;
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
     * Initializes the service by fetching an initial token on application startup.
     */
    @PostConstruct
    public void init() {
        String token = keycloakAdminTokenService.getAccessToken();
        if (token != null) {
            // Log a portion of the token for verification
            String maskedToken = token.substring(0, Math.min(20, token.length())) + "...";
            logger.info("Successfully retrieved Keycloak admin token: {}", maskedToken);
            this.accessToken = token;
        } else {
            logger.warn("Failed to retrieve Keycloak admin token");
        }
    }

    /**
     * Creates a new realm in Keycloak if it does not already exist.
     * @return true if the realm was created successfully or already exists, false otherwise
     */
    public boolean createRealm() {
        String url = keycloakServerUrl + "/admin/realms";

        // First, check if the realm already exists
        List<String> existingRealms = getAllRealms();
        if (existingRealms.contains(realmName)) {
            logger.info("Realm '{}' already exists in Keycloak", realmName);
            return true; // Realm already exists
        }

        // If the realm doesn't exist, create it
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.accessToken);

        Map<String, Object> body = Map.of(
            "realm", realmName,
            "enabled", true,
            "registrationEmailAsUsername", true, // use email as identifier instead of username
            "duplicateEmailsAllowed", false // prevent duplicate emails
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(url, request, Void.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Realm '{}' created successfully in Keycloak", realmName);
                // Create default roles
                addRoleToRealm(defaultClientRole);
                addRoleToRealm(defaultAdminRole);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            logger.error("Error creating realm: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Adds a role to the configured realm in Keycloak.
     * @param roleName the name of the role to add
     * @return true if the role was added successfully, false otherwise
     */
    public boolean addRoleToRealm(String roleName) {
        String roleUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/roles";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.accessToken);

        Map<String, Object> body = Map.of("name", roleName);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(roleUrl, request, Void.class);
            logger.info("Role '{}' added successfully to realm '{}'", roleName, realmName);
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.error("Error adding role to realm: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the configured realm in Keycloak.
     * @return true if the realm was deleted successfully, false otherwise
     */
    public boolean deleteRealm() {
        String deleteUrl = keycloakServerUrl + "/admin/realms/" + realmName;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                deleteUrl, org.springframework.http.HttpMethod.DELETE, request, Void.class);
            logger.info("Realm '{}' deleted successfully from Keycloak", realmName);
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.error("Error deleting realm: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Creates a new user in the configured realm with the default user role.
     * @param username the username of the new user
     * @param password the password for the new user
     * @param email the email of the new user
     * @return true if the user was created successfully, false otherwise
     */
    public boolean createUserInRealm(String password, String email, String surname, String lastname) {

        String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.accessToken);

        Map<String, Object> credentials = Map.of(
            "type", "password",
            "value", password,
            "temporary", false
        );

        Map<String, Object> body = Map.of(
            "enabled", true,
            "email", email,
            "firstName", surname,
            "lastName", lastname,
            "realmRoles", List.of(defaultClientRole),
            "credentials", List.of(credentials)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(userUrl, request, Void.class);
            logger.info("User '{}' '{}' created successfully in realm '{}'", surname, lastname, realmName);
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.error("Error creating user in realm: {}", e.getMessage());
            return false;
        }
    }

    public boolean createAdminUserInRealm(String password, String email, String surname, String lastname) {
        String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(this.accessToken);

        Map<String, Object> credentials = Map.of(
            "type", "password",
            "value", password,
            "temporary", false
        );

        Map<String, Object> body = Map.of(
            "enabled", true,
            "email", email,
            "firstName", surname,
            "lastName", lastname,
            "realmRoles", List.of(defaultAdminRole),
            "credentials", List.of(credentials)
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(userUrl, request, Void.class);
            logger.info("Admin user '{}' '{}' created successfully in realm '{}'", surname, lastname, realmName);
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            logger.error("Error creating admin user in realm: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a user by UUID from the configured realm.
     * @param userId the UUID of the user to retrieve
     * @return the user DTO, or null if not found
     */
    public KeycloakUserResponseDTO retrieveUserByUUID(String userId) {
        String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KeycloakUserResponseDTO> response = restTemplate.exchange(
                userUrl, org.springframework.http.HttpMethod.GET, request, KeycloakUserResponseDTO.class);
            logger.info("User '{}' retrieved successfully from realm '{}'", userId, realmName);
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error retrieving user by UUID: {}", e.getMessage());
            return null;
        }
    }

    public KeycloakUserResponseDTO retrieveUserByEmail(String email) {
        String userUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KeycloakUserResponseDTO[]> response = restTemplate.exchange(
                userUrl, org.springframework.http.HttpMethod.GET, request, KeycloakUserResponseDTO[].class);
            logger.info("User with email '{}' retrieved successfully from realm '{}'", email, realmName);
            KeycloakUserResponseDTO[] users = response.getBody();
            if (users != null && users.length > 0) {
                return users[0]; // Return the first matching user
            } else {
                return null; // No user found
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving user by email: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves all users from the configured realm.
     * @return list of user DTOs, or empty list if none found
     */
    public List<KeycloakUserResponseDTO> retrieveAllUsersInRealm() {
        String usersUrl = keycloakServerUrl + "/admin/realms/" + realmName + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<KeycloakUserResponseDTO[]> response = restTemplate.exchange(
                usersUrl, org.springframework.http.HttpMethod.GET, request, KeycloakUserResponseDTO[].class);
            logger.info("All users retrieved successfully from realm '{}'", realmName);
            return response.getBody() != null 
                ? Arrays.asList(response.getBody()) 
                : Collections.emptyList();
            
        } catch (Exception e) {
            logger.error("Error retrieving all users in realm: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns the configured realm name.
     * @return the realm name
     */
    public String getRealmName() {
        return realmName;
    }

    /**
     * Retrieves all realms from Keycloak.
     * @return list of realm names, or empty list if none found
     */
    public List<String> getAllRealms() {
        String realmsUrl = keycloakServerUrl + "/admin/realms";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(this.accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map[]> response = restTemplate.exchange(
                realmsUrl, org.springframework.http.HttpMethod.GET, request, Map[].class);
            logger.info("All realms retrieved successfully");
            if (response.getBody() != null) {
                return Arrays.stream(response.getBody())
                        .map(realm -> (String) realm.get("realm"))
                        .toList();
            } else {
                return Collections.emptyList();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving all realms: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
