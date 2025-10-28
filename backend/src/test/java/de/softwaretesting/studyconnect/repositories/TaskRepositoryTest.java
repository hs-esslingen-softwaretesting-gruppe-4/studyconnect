package de.softwaretesting.studyconnect.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;

/**
 * Data JPA tests for Task entity covering:
 * - object creation with valid data
 * - validation constraints (title not null)
 * - relationships (createdBy, assignees)
 * - helper methods and business logic (tags, assignment, status transitions, overdue and priority)
 */
@DataJpaTest
@ActiveProfiles("test")
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateAndSaveTaskWithValidData() {
        // Arrange
        User creator = new User();
        creator.setEmail("creator@example.com");
        creator.setSurname("Creator");
        creator.setLastname("User");
        creator.setCreatedAt(LocalDateTime.now());
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
        assertThrows(DataIntegrityViolationException.class, () -> {
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
        creator.setCreatedAt(LocalDateTime.now());
        userRepository.saveAndFlush(creator);

        User assignee1 = new User();
        assignee1.setEmail("a1@example.com");
        assignee1.setSurname("A1");
        assignee1.setLastname("User");
        assignee1.setCreatedAt(LocalDateTime.now());
        userRepository.saveAndFlush(assignee1);

        User assignee2 = new User();
        assignee2.setEmail("a2@example.com");
        assignee2.setSurname("A2");
        assignee2.setLastname("User");
        assignee2.setCreatedAt(LocalDateTime.now());
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

}
