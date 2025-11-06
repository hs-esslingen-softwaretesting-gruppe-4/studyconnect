package de.softwaretesting.studyconnect.repositories;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.softwaretesting.studyconnect.models.User;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    /**
     * Tests that a User with valid data can be created and saved successfully.
     */
    @Test
    void shouldCreateAndSaveUserWithValidData() {

        // Arrange & Act
        User user = new User();
        user.setEmail("test@example.com");
        user.setSurname("John");
        user.setLastname("Doe");
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        // Assert
        assertNotNull(saved.getId());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("John", saved.getSurname());
        assertEquals("Doe", saved.getLastname());
    }

    /**
     * Tests that saving a User with a null email field fails due to the not-null constraint.
     */
    @Test
    void shouldFailToSaveUserWhenEmailIsNull() {
        // Arrange
        User user = new User();
        user.setSurname("John");
        user.setLastname("Doe");
        user.setCreatedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(user));
    }

    /**
     * Tests that saving a User with a null surname field fails due to the not-null constraint.
     */
    @Test
    void shouldFailToSaveUserWhenSurnameIsNull() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setLastname("Doe");
        user.setCreatedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(user));
    }

    /**
     * Tests that saving a User with a null lastname field fails due to the not-null constraint.
     */
    @Test
    void shouldFailToSaveUserWhenLastnameIsNull() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setSurname("John");
        user.setCreatedAt(LocalDateTime.now());

        // Act & Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(user));
    }

    /**
     * Tests that saving a User with a duplicate Keycloak UUID fails due to the unique constraint.
     */
    @Test
    void shouldFailToSaveUserWithDuplicateKeycloakUUID() {
        User user1 = new User();
        user1.setEmail("user1@example.com");
        user1.setSurname("John");
        user1.setLastname("Doe");
        user1.setCreatedAt(LocalDateTime.now());
        user1.setKeycloakUUID("uuid-123");
        userRepository.saveAndFlush(user1);

        User user2 = new User();
        user2.setEmail("user2@example.com");
        user2.setSurname("Jane");
        user2.setLastname("Doe");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setKeycloakUUID("uuid-123");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(user2));
    }

}
