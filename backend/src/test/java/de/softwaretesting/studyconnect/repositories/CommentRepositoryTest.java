package de.softwaretesting.studyconnect.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.softwaretesting.studyconnect.models.Comment;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryTest {

  @Autowired private CommentRepository commentRepository;

  @Autowired private GroupRepository groupRepository;

  @Autowired private UserRepository userRepository;

  private User savedUser;
  private User savedAdmin;
  private Group savedGroup;

  /** Tests that a Comment object can be successfully created and saved. */
  @Test
  void shouldCreateAndSaveCommentWithValidData() {

    // Arange User and Group
    User user = new User();
    user.setEmail("test@example.com");
    user.setFirstname("John");
    user.setLastname("Doe");

    savedUser = userRepository.save(user);

    // Arrange: create and save an admin user
    User admin = new User();
    admin.setEmail("admin@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("User");
    savedAdmin = userRepository.save(admin);

    // Arrange: create a new group
    Group group = new Group();
    group.setName("Study Group 1");
    group.setDescription("Test group for StudyConnect");
    group.setPublic(true);
    group.setMaxMembers(10);
    group.setCreatedBy(savedAdmin);
    group.setAdmin(savedAdmin);

    // Add the admin as a member
    Set<User> members = new HashSet<>();
    members.add(savedAdmin);
    group.setMembers(members);

    // Act: save the group
    savedGroup = groupRepository.save(group);

    // Create & fill Comment object
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent("This is a text with 1 number and a special €haracter.");

    // Save Comment
    Comment saved = commentRepository.save(comment);

    // Assertion
    assertNotNull(saved.getId());
    assertNotNull(saved.getCreatedBy());
    assertNotNull(saved.getCreatedIn());
    assertNotNull(saved.getCreatedAt());
    assertNotNull(saved.getUpdatedAt());
    assertEquals("This is a text with 1 number and a special €haracter.", saved.getContent());
    assertEquals(savedUser, saved.getCreatedBy());
    assertEquals(savedGroup, saved.getCreatedIn());
  }

  /*
   *  Test that saving a comment with a null createdBy
   */
  @Test
  void shouldFailToSaveCommentWhenCreatedByIsNull() {
    // Arrange: create and save an admin user for the group
    User admin = new User();
    admin.setEmail("admin-null-test@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("User");
    savedAdmin = userRepository.save(admin);

    // Arrange: create a group (Group requires createdBy)
    Group group = new Group();
    group.setName("Test Group for Null CreatedBy");
    group.setDescription("A group to test null createdBy in comments");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    Comment comment = new Comment();
    comment.setCreatedIn(savedGroup);

    assertThrows(
        DataIntegrityViolationException.class, () -> commentRepository.saveAndFlush(comment));
  }

  /*
   *  Test that saving a comment with a null createdIn
   */
  @Test
  void shouldFailToSaveCommentWhenCreatedInIsNull() {
    // Arrange: create and save a user
    User user = new User();
    user.setEmail("user-null-test@example.com");
    user.setFirstname("Test");
    user.setLastname("User");
    savedUser = userRepository.save(user);

    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);

    assertThrows(
        DataIntegrityViolationException.class, () -> commentRepository.saveAndFlush(comment));
  }

  // ========== BOUNDARY VALUE ANALYSIS TESTS ==========

  /**
   * Tests saving a comment with very long content (Text field). Text columns can typically store
   * very large amounts of data.
   */
  @Test
  void shouldHandleVeryLongContent() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("longcontent@example.com");
    user.setFirstname("Long");
    user.setLastname("Content");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-long@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Long");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Long Content Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment with 10,000 characters
    String longContent = "A".repeat(10000);
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent(longContent);

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(10000, saved.getContent().length());
  }

  /** Tests saving a comment with minimal content (1 character). */
  @Test
  void shouldHandleMinimalContent() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("minimalcontent@example.com");
    user.setFirstname("Min");
    user.setLastname("Content");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-min@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Min");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Min Content Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment with 1 character
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent("X");

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(1, saved.getContent().length());
  }

  // ========== EQUIVALENCE CLASS PARTITIONING TESTS ==========

  /** Tests that NULL content is allowed (optional field). */
  @Test
  void shouldAllowNullContent() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("nullcontent@example.com");
    user.setFirstname("Null");
    user.setLastname("Content");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-nullcontent@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Null");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Null Content Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment with NULL content
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent(null);

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(null, saved.getContent());
  }

  /** Tests that empty string content is allowed. */
  @Test
  void shouldAllowEmptyContent() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("emptycontent@example.com");
    user.setFirstname("Empty");
    user.setLastname("Content");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-empty@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Empty");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Empty Content Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment with empty content
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent("");

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("", saved.getContent());
  }

  // ========== EDGE CASE TESTS ==========

  /** Tests that createdAt and updatedAt are automatically set. */
  @Test
  void shouldAutoSetTimestamps() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("timestamp@example.com");
    user.setFirstname("Time");
    user.setLastname("Stamp");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-timestamp@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Time");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Timestamp Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent("Test timestamp");

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertNotNull(saved.getCreatedAt(), "createdAt should be auto-set");
    assertNotNull(saved.getUpdatedAt(), "updatedAt should be auto-set");
  }

  /** Tests content with special characters and unicode. */
  @Test
  void shouldHandleSpecialCharactersAndUnicode() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("unicode@example.com");
    user.setFirstname("Unicode");
    user.setLastname("Test");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-unicode@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Unicode");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Unicode Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment with unicode and special chars
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent("Hello 世界! This is a test with émojis 😀 and symbols: €$¥£");

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("Hello 世界! This is a test with émojis 😀 and symbols: €$¥£", saved.getContent());
  }

  /** Tests content with newlines and whitespace. */
  @Test
  void shouldPreserveWhitespaceAndNewlines() {
    // Arrange: setup user and group
    User user = new User();
    user.setEmail("whitespace@example.com");
    user.setFirstname("White");
    user.setLastname("Space");
    savedUser = userRepository.save(user);

    User admin = new User();
    admin.setEmail("admin-whitespace@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("White");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Whitespace Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create comment with newlines and whitespace
    String contentWithWhitespace = "Line 1\n\nLine 3 with    spaces\n\tTabbed line";
    Comment comment = new Comment();
    comment.setCreatedBy(savedUser);
    comment.setCreatedIn(savedGroup);
    comment.setContent(contentWithWhitespace);

    // Act
    Comment saved = commentRepository.saveAndFlush(comment);

    // Assert
    assertEquals(contentWithWhitespace, saved.getContent());
  }

  /** Tests multiple comments in the same group by different users. */
  @Test
  void shouldAllowMultipleCommentsInSameGroup() {
    // Arrange: setup users and group
    User user1 = new User();
    user1.setEmail("user1@example.com");
    user1.setFirstname("User1");
    user1.setLastname("Test");
    User savedUser1 = userRepository.save(user1);

    User user2 = new User();
    user2.setEmail("user2@example.com");
    user2.setFirstname("User2");
    user2.setLastname("Test");
    User savedUser2 = userRepository.save(user2);

    User admin = new User();
    admin.setEmail("admin-multi@example.com");
    admin.setFirstname("Admin");
    admin.setLastname("Multi");
    savedAdmin = userRepository.save(admin);

    Group group = new Group();
    group.setName("Multi Comment Group");
    group.setCreatedBy(savedAdmin);
    savedGroup = groupRepository.save(group);

    // Arrange: create multiple comments
    Comment comment1 = new Comment();
    comment1.setCreatedBy(savedUser1);
    comment1.setCreatedIn(savedGroup);
    comment1.setContent("First comment");

    Comment comment2 = new Comment();
    comment2.setCreatedBy(savedUser2);
    comment2.setCreatedIn(savedGroup);
    comment2.setContent("Second comment");

    Comment comment3 = new Comment();
    comment3.setCreatedBy(savedUser1);
    comment3.setCreatedIn(savedGroup);
    comment3.setContent("Third comment");

    // Act
    Comment saved1 = commentRepository.saveAndFlush(comment1);
    Comment saved2 = commentRepository.saveAndFlush(comment2);
    Comment saved3 = commentRepository.saveAndFlush(comment3);

    // Assert
    assertNotNull(saved1.getId());
    assertNotNull(saved2.getId());
    assertNotNull(saved3.getId());
    assertEquals(savedGroup.getId(), saved1.getCreatedIn().getId());
    assertEquals(savedGroup.getId(), saved2.getCreatedIn().getId());
    assertEquals(savedGroup.getId(), saved3.getCreatedIn().getId());
  }
}
