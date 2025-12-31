package de.softwaretesting.studyconnect.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.softwaretesting.studyconnect.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  /** Tests that a User with valid data can be created and saved successfully. */
  @Test
  void shouldCreateAndSaveUserWithValidData() {

    // Arrange & Act
    User user = new User();
    user.setEmail("test@example.com");
    user.setFirstname("John");
    user.setLastname("Doe");

    User saved = userRepository.save(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("test@example.com", saved.getEmail());
    assertEquals("John", saved.getFirstname());
    assertEquals("Doe", saved.getLastname());
  }

  /** Tests that saving a User with a null email field fails due to the not-null constraint. */
  @Test
  void shouldFailToSaveUserWhenEmailIsNull() {
    // Arrange
    User user = new User();
    user.setFirstname("John");
    user.setLastname("Doe");

    // Act & Assert
    assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user));
  }

  /** Tests that saving a User with a null firstname field fails due to the not-null constraint. */
  @Test
  void shouldFailToSaveUserWhenFirstnameIsNull() {
    // Arrange
    User user = new User();
    user.setEmail("test@example.com");
    user.setLastname("Doe");

    // Act & Assert
    assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user));
  }

  /** Tests that saving a User with a null lastname field fails due to the not-null constraint. */
  @Test
  void shouldFailToSaveUserWhenLastnameIsNull() {
    // Arrange
    User user = new User();
    user.setEmail("test@example.com");
    user.setFirstname("John");

    // Act & Assert
    assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user));
  }

  /** Tests that saving a User with a duplicate Keycloak UUID fails due to the unique constraint. */
  @Test
  void shouldFailToSaveUserWithDuplicateKeycloakUUID() {
    String uniqueUUID = "test-duplicate-uuid-" + System.nanoTime();

    User user1 = new User();
    user1.setEmail("user1@example.com");
    user1.setFirstname("John");
    user1.setLastname("Doe");
    user1.setKeycloakUUID(uniqueUUID);
    userRepository.saveAndFlush(user1);

    User user2 = new User();
    user2.setEmail("user2@example.com");
    user2.setFirstname("Jane");
    user2.setLastname("Doe");
    user2.setKeycloakUUID(uniqueUUID);

    assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));
  }

  /** Tests that saving a User with a duplicate email fails due to the unique constraint. */
  @Test
  void shouldFailToSaveUserWithDuplicateEmail() {
    String duplicateEmail = "duplicate-email@example.com";

    User user1 = new User();
    user1.setEmail(duplicateEmail);
    user1.setFirstname("John");
    user1.setLastname("Doe");
    userRepository.saveAndFlush(user1);

    User user2 = new User();
    user2.setEmail(duplicateEmail);
    user2.setFirstname("Jane");
    user2.setLastname("Doe");

    assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(user2));
  }

  // ========== BOUNDARY VALUE ANALYSIS TESTS ==========

  /**
   * Tests that a User can be saved with very long but valid field values. Ensures the system
   * handles boundary conditions for text fields.
   */
  @Test
  void shouldHandleLongFieldValues() {
    // Arrange - create user with long field values
    User user = new User();
    user.setEmail("very.long.email.address.for.testing.purposes@example.com");
    user.setFirstname("VeryLongFirstnameWithManyCharactersToTestBoundaries");
    user.setLastname("VeryLongLastnameWithManyCharactersToTestBoundaries");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("very.long.email.address.for.testing.purposes@example.com", saved.getEmail());
  }

  /** Tests minimum valid length for name fields (1 character). */
  @Test
  void shouldHandleMinimumLengthNames() {
    // Arrange - single character names (boundary)
    User user = new User();
    user.setEmail("min@example.com");
    user.setFirstname("J");
    user.setLastname("D");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("J", saved.getFirstname());
    assertEquals("D", saved.getLastname());
  }

  // ========== EQUIVALENCE CLASS PARTITIONING TESTS ==========

  /**
   * Tests that empty string values for email are allowed. Empty strings are a different equivalence
   * class from NULL.
   */
  @Test
  void shouldAllowEmptyEmail() {
    // Arrange - empty email (valid in database)
    User user = new User();
    user.setEmail("");
    user.setFirstname("John");
    user.setLastname("Doe");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("", saved.getEmail());
  }

  /** Tests that empty string values for firstname are allowed. */
  @Test
  void shouldAllowEmptyFirstname() {
    // Arrange - empty firstname
    User user = new User();
    user.setEmail("test@example.com");
    user.setFirstname("");
    user.setLastname("Doe");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("", saved.getFirstname());
  }

  /** Tests that empty string values for lastname are allowed. */
  @Test
  void shouldAllowEmptyLastname() {
    // Arrange - empty lastname
    User user = new User();
    user.setEmail("test@example.com");
    user.setFirstname("John");
    user.setLastname("");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("", saved.getLastname());
  }

  /**
   * Tests that keycloakUUID can be NULL (optional field). NULL is a valid equivalence class for
   * this field.
   */
  @Test
  void shouldAllowNullKeycloakUUID() {
    // Arrange - NULL keycloakUUID is valid
    User user = new User();
    user.setEmail("nulluuid@example.com");
    user.setFirstname("Null");
    user.setLastname("UUID");
    user.setKeycloakUUID(null);

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(null, saved.getKeycloakUUID());
  }

  /** Tests that keycloakUUID can be an empty string. */
  @Test
  void shouldAllowEmptyKeycloakUUID() {
    // Arrange - empty string keycloakUUID
    User user = new User();
    user.setEmail("emptyuuid@example.com");
    user.setFirstname("Empty");
    user.setLastname("UUID");
    user.setKeycloakUUID("");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("", saved.getKeycloakUUID());
  }

  // ========== EDGE CASE TESTS ==========

  /** Tests email with special characters (valid format). */
  @Test
  void shouldHandleEmailWithSpecialCharacters() {
    // Arrange - valid email with special chars
    User user = new User();
    user.setEmail("user+tag@sub.example.com");
    user.setFirstname("Special");
    user.setLastname("Email");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("user+tag@sub.example.com", saved.getEmail());
  }

  /** Tests names with special characters and unicode. */
  @Test
  void shouldHandleNamesWithUnicodeAndSpecialChars() {
    // Arrange - names with unicode and special characters
    User user = new User();
    user.setEmail("unicode@example.com");
    user.setFirstname("François");
    user.setLastname("Müller-Schmidt");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("François", saved.getFirstname());
    assertEquals("Müller-Schmidt", saved.getLastname());
  }

  /** Tests that createdAt timestamp is automatically set. */
  @Test
  void shouldAutoSetCreatedAt() {
    // Arrange
    User user = new User();
    user.setEmail("timestamp@example.com");
    user.setFirstname("Time");
    user.setLastname("Stamp");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertNotNull(saved.getCreatedAt(), "createdAt should be auto-set by @PrePersist");
  }

  /** Tests whitespace handling in names (should be preserved). */
  @Test
  void shouldPreserveWhitespaceInNames() {
    // Arrange - names with whitespace
    User user = new User();
    user.setEmail("whitespace@example.com");
    user.setFirstname("John Paul");
    user.setLastname("Van Der Berg");

    // Act
    User saved = userRepository.saveAndFlush(user);

    // Assert
    assertEquals("John Paul", saved.getFirstname());
    assertEquals("Van Der Berg", saved.getLastname());
  }
}
