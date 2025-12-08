package de.softwaretesting.studyconnect.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Data JPA tests for Task entity covering: - object creation with valid data - validation
 * constraints (title not null) - relationships (createdBy, assignees) - helper methods and business
 * logic (tags, assignment, status transitions, overdue and priority)
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

  @Autowired private TaskRepository taskRepository;

  @Autowired private UserRepository userRepository;

  @Test
  void shouldCreateAndSaveTaskWithValidData() {
    // Arrange
    User creator = new User();
    creator.setEmail("creator@example.com");
    creator.setSurname("Creator");
    creator.setLastname("User");
    User savedCreator = userRepository.save(creator);

    Task task = new Task();
    task.setTitle("Write tests");
    task.setDescription("Write unit tests for Task entity");
    task.setDueDate(LocalDateTime.now().plusDays(3));
    task.setCreatedBy(savedCreator);

    // Act
    Task saved = taskRepository.save(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("Write tests", saved.getTitle());
    assertNotNull(saved.getCreatedAt(), "createdAt should be set by @PrePersist");
    // default priority is MEDIUM
    assertEquals(Task.Priority.MEDIUM, saved.getPriority());
    // default status is OPEN
    assertEquals(Task.Status.OPEN, saved.getStatus());
  }

  @Test
  void shouldEnforceTitleNotNull() {
    // Arrange
    Task task = new Task();
    task.setDescription("No title");

    // Act & Assert
    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          taskRepository.saveAndFlush(task);
        });
  }

  @Test
  void shouldManageAssigneesAndTagsAndRelationships() {
    // Arrange
    User creator = new User();
    creator.setEmail("c@example.com");
    creator.setSurname("C");
    creator.setLastname("User");
    userRepository.saveAndFlush(creator);

    User assignee1 = new User();
    assignee1.setEmail("a1@example.com");
    assignee1.setSurname("A1");
    assignee1.setLastname("User");
    userRepository.saveAndFlush(assignee1);

    User assignee2 = new User();
    assignee2.setEmail("a2@example.com");
    assignee2.setSurname("A2");
    assignee2.setLastname("User");
    userRepository.saveAndFlush(assignee2);

    Task task = new Task();
    task.setTitle("Collaborative task");
    task.setCreatedBy(creator);
    task.addAssignee(assignee1);
    task.addAssignee(assignee2);
    task.addTag("backend");
    task.addTag("tests");

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    Optional<Task> reloaded = taskRepository.findById(saved.getId());
    assertTrue(reloaded.isPresent());
    Task t = reloaded.get();

    // Assert relationships
    assertEquals(2, t.getAssignees().size(), "Should have two assignees");
    assertEquals(2, t.getTags().size(), "Should have two tags");
    assertEquals(creator.getEmail(), t.getCreatedBy().getEmail(), "creator relation persisted");

    // Test removal helpers
    t.removeAssignee(assignee2);
    t.removeTag("tests");
    Task updated = taskRepository.saveAndFlush(t);
    assertEquals(1, updated.getAssignees().size());
    assertEquals(1, updated.getTags().size());
  }

  @Test
  void shouldSupportStatusTransitionAndMarkComplete() throws InterruptedException {
    // Arrange
    Task task = new Task();
    task.setTitle("Complete me");
    Task saved = taskRepository.saveAndFlush(task);

    // Precondition
    assertEquals(Task.Status.OPEN, saved.getStatus());

    // Act - transition to completed via helper
    saved.markComplete();
    Task updated = taskRepository.saveAndFlush(saved);

    // Assert
    assertEquals(Task.Status.COMPLETED, updated.getStatus());
    assertNotNull(updated.getUpdatedAt(), "updatedAt should be set on markComplete");
  }

  @Test
  void shouldDetectOverdue() {
    // Arrange
    Task task = new Task();
    task.setTitle("Past task");
    task.setDueDate(LocalDateTime.now().minus(2, ChronoUnit.DAYS));
    Task saved = taskRepository.saveAndFlush(task);

    // Assert business logic
    assertTrue(saved.isOverdue(), "Task with past due date and not completed should be overdue");

    // Mark complete -> not overdue
    saved.markComplete();
    Task updated = taskRepository.saveAndFlush(saved);
    assertTrue(!updated.isOverdue(), "Completed task should not be considered overdue");
  }

  @Test
  void shouldManagePriorityDefaultAndUpdates() {
    // Arrange
    Task task = new Task();
    task.setTitle("Priority task");
    Task saved = taskRepository.saveAndFlush(task);

    // default priority
    assertEquals(Task.Priority.MEDIUM, saved.getPriority());

    // update priority
    saved.setPriority(Task.Priority.HIGH);
    Task updated = taskRepository.saveAndFlush(saved);
    assertEquals(Task.Priority.HIGH, updated.getPriority());
  }

  // ========== BOUNDARY VALUE ANALYSIS TESTS ==========

  @Test
  void shouldHandleTitleAtMaxLength() {
    // Arrange - title at exactly 200 characters (boundary)
    String title200 = "A".repeat(200);
    Task task = new Task();
    task.setTitle(title200);

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(200, saved.getTitle().length());
  }

  @Test
  void shouldHandleTitleAtMinLength() {
    // Arrange - title at 1 character (minimum valid)
    Task task = new Task();
    task.setTitle("X");

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(1, saved.getTitle().length());
  }

  @Test
  void shouldHandleDescriptionAtMaxLength() {
    // Arrange - description at exactly 1000 characters (boundary)
    String desc1000 = "B".repeat(1000);
    Task task = new Task();
    task.setTitle("Max description task");
    task.setDescription(desc1000);

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(1000, saved.getDescription().length());
  }

  @Test
  void shouldHandleEmptyDescription() {
    // Arrange - empty description (valid equivalence class)
    Task task = new Task();
    task.setTitle("No description");
    task.setDescription("");

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals("", saved.getDescription());
  }

  @Test
  void shouldHandleNullDescription() {
    // Arrange - NULL description (valid equivalence class)
    Task task = new Task();
    task.setTitle("NULL description");
    task.setDescription(null);

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(null, saved.getDescription());
  }

  // ========== EQUIVALENCE CLASS PARTITIONING TESTS ==========

  @Test
  void shouldHandleNullDueDate() {
    // Arrange - NULL due date (valid equivalence class)
    Task task = new Task();
    task.setTitle("No deadline");
    task.setDueDate(null);

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertEquals(null, saved.getDueDate());
    assertTrue(!saved.isOverdue(), "Task without due date should not be overdue");
  }

  @Test
  void shouldHandleFutureDueDate() {
    // Arrange - future due date (valid equivalence class)
    Task task = new Task();
    task.setTitle("Future deadline");
    task.setDueDate(LocalDateTime.now().plusDays(7));

    // Act
    Task saved = taskRepository.saveAndFlush(task);

    // Assert
    assertNotNull(saved.getId());
    assertTrue(!saved.isOverdue(), "Task with future due date should not be overdue");
  }

  @Test
  void shouldHandleAllPriorityValues() {
    // Test all priority enum values (exhaustive partitioning)
    for (Task.Priority priority : Task.Priority.values()) {
      Task task = new Task();
      task.setTitle("Priority " + priority.name());
      task.setPriority(priority);

      Task saved = taskRepository.saveAndFlush(task);

      assertEquals(priority, saved.getPriority());
    }
  }

  @Test
  void shouldHandleAllStatusValues() {
    // Test all status enum values (exhaustive partitioning)
    for (Task.Status status : Task.Status.values()) {
      Task task = new Task();
      task.setTitle("Status " + status.name());
      task.setStatus(status);

      Task saved = taskRepository.saveAndFlush(task);

      assertEquals(status, saved.getStatus());
    }
  }

  // ========== DECISION TABLE TESTS ==========

  @Test
  void shouldTransitionThroughAllStatuses() {
    // Decision table: status transitions
    Task task = new Task();
    task.setTitle("Status transition task");
    Task saved = taskRepository.saveAndFlush(task);

    // OPEN → IN_PROGRESS
    assertEquals(Task.Status.OPEN, saved.getStatus());
    saved.setInProgress();
    Task updated1 = taskRepository.saveAndFlush(saved);
    assertEquals(Task.Status.IN_PROGRESS, updated1.getStatus());

    // IN_PROGRESS → COMPLETED
    updated1.markComplete();
    Task updated2 = taskRepository.saveAndFlush(updated1);
    assertEquals(Task.Status.COMPLETED, updated2.getStatus());

    // Test CANCELLED status
    Task cancelTask = new Task();
    cancelTask.setTitle("To be cancelled");
    cancelTask.setStatus(Task.Status.CANCELLED);
    Task cancelled = taskRepository.saveAndFlush(cancelTask);
    assertEquals(Task.Status.CANCELLED, cancelled.getStatus());
  }

  @Test
  void shouldHandleOverdueDecisionTable() {
    // Decision table for isOverdue():
    // | Status     | DueDate | Expected |
    // |------------|---------|----------|
    // | COMPLETED  | Past    | false    |
    // | COMPLETED  | Future  | false    |
    // | COMPLETED  | NULL    | false    |
    // | OPEN       | Past    | true     |
    // | OPEN       | Future  | false    |
    // | OPEN       | NULL    | false    |

    LocalDateTime past = LocalDateTime.now().minusDays(5);
    LocalDateTime future = LocalDateTime.now().plusDays(5);

    // Row 1: COMPLETED + Past → false
    Task t1 = new Task();
    t1.setTitle("Completed past");
    t1.setStatus(Task.Status.COMPLETED);
    t1.setDueDate(past);
    Task saved1 = taskRepository.saveAndFlush(t1);
    assertTrue(!saved1.isOverdue());

    // Row 2: COMPLETED + Future → false
    Task t2 = new Task();
    t2.setTitle("Completed future");
    t2.setStatus(Task.Status.COMPLETED);
    t2.setDueDate(future);
    Task saved2 = taskRepository.saveAndFlush(t2);
    assertTrue(!saved2.isOverdue());

    // Row 3: COMPLETED + NULL → false
    Task t3 = new Task();
    t3.setTitle("Completed no date");
    t3.setStatus(Task.Status.COMPLETED);
    t3.setDueDate(null);
    Task saved3 = taskRepository.saveAndFlush(t3);
    assertTrue(!saved3.isOverdue());

    // Row 4: OPEN + Past → true
    Task t4 = new Task();
    t4.setTitle("Open past");
    t4.setStatus(Task.Status.OPEN);
    t4.setDueDate(past);
    Task saved4 = taskRepository.saveAndFlush(t4);
    assertTrue(saved4.isOverdue());

    // Row 5: OPEN + Future → false
    Task t5 = new Task();
    t5.setTitle("Open future");
    t5.setStatus(Task.Status.OPEN);
    t5.setDueDate(future);
    Task saved5 = taskRepository.saveAndFlush(t5);
    assertTrue(!saved5.isOverdue());

    // Row 6: OPEN + NULL → false
    Task t6 = new Task();
    t6.setTitle("Open no date");
    t6.setStatus(Task.Status.OPEN);
    t6.setDueDate(null);
    Task saved6 = taskRepository.saveAndFlush(t6);
    assertTrue(!saved6.isOverdue());
  }

  // ========== EDGE CASE TESTS ==========

  @Test
  void shouldHandleDuplicateTagAddition() {
    // Edge case: adding the same tag multiple times
    Task task = new Task();
    task.setTitle("Duplicate tags");
    task.addTag("backend");
    task.addTag("backend"); // duplicate
    task.addTag("backend"); // duplicate

    Task saved = taskRepository.saveAndFlush(task);

    // Set should contain only one instance
    assertEquals(1, saved.getTags().size());
    assertTrue(saved.getTags().contains("backend"));
  }

  @Test
  void shouldHandleDuplicateAssigneeAddition() {
    // Edge case: adding the same assignee multiple times
    User user = new User();
    user.setEmail("assignee@example.com");
    user.setSurname("Assignee");
    user.setLastname("User");
    User savedUser = userRepository.saveAndFlush(user);

    Task task = new Task();
    task.setTitle("Duplicate assignees");
    task.addAssignee(savedUser);
    task.addAssignee(savedUser); // duplicate

    Task saved = taskRepository.saveAndFlush(task);

    // Set should contain only one instance
    assertEquals(1, saved.getAssignees().size());
    assertTrue(saved.getAssignees().contains(savedUser));
  }

  @Test
  void shouldHandleNullSafetyInHelperMethods() {
    // Edge case: NULL safety in helper methods
    Task task = new Task();
    task.setTitle("NULL safety test");

    // Should not throw exceptions
    task.addAssignee(null);
    task.removeAssignee(null);
    task.addTag(null);
    task.removeTag(null);
    task.addTag("  "); // blank tag

    Task saved = taskRepository.saveAndFlush(task);

    // Should have no assignees or tags
    assertEquals(0, saved.getAssignees().size());
    assertEquals(0, saved.getTags().size());
  }

  @Test
  void shouldHandleTagTrimming() {
    // Edge case: whitespace handling in tags
    Task task = new Task();
    task.setTitle("Trimmed tags");
    task.addTag("  frontend  ");
    task.addTag("frontend"); // should be same after trim

    Task saved = taskRepository.saveAndFlush(task);

    // Should deduplicate after trimming
    assertEquals(1, saved.getTags().size());
    assertTrue(saved.getTags().contains("frontend"));
  }

  @Test
  void shouldHandleMultipleTagsAndAssignees() {
    // Edge case: many tags and assignees
    User u1 = new User();
    u1.setEmail("u1@example.com");
    u1.setSurname("U1");
    u1.setLastname("User");
    userRepository.saveAndFlush(u1);

    User u2 = new User();
    u2.setEmail("u2@example.com");
    u2.setSurname("U2");
    u2.setLastname("User");
    userRepository.saveAndFlush(u2);

    User u3 = new User();
    u3.setEmail("u3@example.com");
    u3.setSurname("U3");
    u3.setLastname("User");
    userRepository.saveAndFlush(u3);

    Task task = new Task();
    task.setTitle("Many relationships");
    task.addAssignee(u1);
    task.addAssignee(u2);
    task.addAssignee(u3);
    task.addTag("tag1");
    task.addTag("tag2");
    task.addTag("tag3");
    task.addTag("tag4");
    task.addTag("tag5");

    Task saved = taskRepository.saveAndFlush(task);

    assertEquals(3, saved.getAssignees().size());
    assertEquals(5, saved.getTags().size());
  }

  @Test
  void shouldHandleEmptyCategory() {
    // Edge case: empty vs NULL category
    Task task1 = new Task();
    task1.setTitle("Empty category");
    task1.setCategory("");

    Task task2 = new Task();
    task2.setTitle("NULL category");
    task2.setCategory(null);

    Task saved1 = taskRepository.saveAndFlush(task1);
    Task saved2 = taskRepository.saveAndFlush(task2);

    assertEquals("", saved1.getCategory());
    assertEquals(null, saved2.getCategory());
  }
}
