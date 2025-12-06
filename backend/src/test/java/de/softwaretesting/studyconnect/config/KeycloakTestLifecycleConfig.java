package de.softwaretesting.studyconnect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.services.KeycloakService;
import de.softwaretesting.studyconnect.services.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Test configuration that manages Keycloak realm lifecycle for integration tests.
 * Creates the test realm and test users before tests run, and cleans up after.
 */
@TestConfiguration
@Profile("test")
public class KeycloakTestLifecycleConfig {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakTestLifecycleConfig.class);

    private final KeycloakService keycloakService;
    private final UserService userService;

    @Value("${test.user.email}")
    private String testUserEmail;

    @Value("${test.user.password}")
    private String testUserPassword;

    @Value("${test.user.surname}")
    private String testUserSurname;

    @Value("${test.user.lastname}")
    private String testUserLastname;

    @Value("${test.admin.email}")
    private String testAdminEmail;

    @Value("${test.admin.password}")
    private String testAdminPassword;

    @Value("${test.admin.surname}")
    private String testAdminSurname;

    @Value("${test.admin.lastname}")
    private String testAdminLastname;

    public KeycloakTestLifecycleConfig(KeycloakService keycloakService, UserService userService) {
        this.keycloakService = keycloakService;
        this.userService = userService;
    }

    @PostConstruct
    public void setupTestRealm() {
        logger.info("Setting up Keycloak test realm: {}", keycloakService.getRealmName());

        // Create the test realm
        boolean realmCreated = keycloakService.createRealm();
        if (!realmCreated) {
            logger.error("Failed to create test realm");
            return;
        }

        // Create test user
        try {
            UserCreateRequestDTO testUser = new UserCreateRequestDTO(
                testUserSurname,
                testUserLastname,
                testUserPassword,
                testUserEmail
            );
            userService.createUser(testUser);
            logger.info("Test user created: {}", testUserEmail);
        } catch (Exception e) {
            logger.warn("Could not create test user (may already exist): {}", e.getMessage());
        }

        // Create test admin
        try {
            UserCreateRequestDTO testAdmin = new UserCreateRequestDTO(
                testAdminSurname,
                testAdminLastname,
                testAdminPassword,
                testAdminEmail
            );
            userService.createAdmin(testAdmin);
            logger.info("Test admin created: {}", testAdminEmail);
        } catch (Exception e) {
            logger.warn("Could not create test admin (may already exist): {}", e.getMessage());
        }

        logger.info("Keycloak test realm setup completed");
    }

    @PreDestroy
    public void teardownTestRealm() {
        logger.info("Tearing down Keycloak test realm: {}", keycloakService.getRealmName());

        boolean deleted = keycloakService.deleteRealm();
        if (deleted) {
            logger.info("Test realm deleted successfully");
        } else {
            logger.warn("Failed to delete test realm");
        }
    }
}
