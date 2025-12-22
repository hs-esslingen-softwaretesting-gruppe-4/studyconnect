package de.softwaretesting.studyconnect.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class TaskIntegrationTest {
  @Autowired private TaskRepository taskRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private GroupRepository groupRepository;

  private User user1;
  private User user2;
  private Group group;
  private Task task;

  @BeforeEach
  void setup() {

    // Set up users
    user1 = new User();
    user1.setEmail("creator@example.com");
    user1.setSurname("Creator");
    user1.setLastname("User");
    userRepository.save(user1);

    user2 = new User();
    user2.setEmail("assignee1@example.com");
    user2.setSurname("Assignee");
    user2.setLastname("One");
    userRepository.save(user2);

    // Set up group
    group = new Group();
    group.setName("Test Group");
    group.setCreatedBy(user1);
    groupRepository.save(group);

    // Set up task
    task = new Task();
    task.setTitle("Test Task");
    task.setDescription("A sample task");
    task.setPriority(Task.Priority.MEDIUM);
    task.setStatus(Task.Status.OPEN);
    task.setDueDate(LocalDateTime.now().plusDays(1));
    task.setCreatedBy(user1);
    task.setGroup(group);

    task.addAssignee(user1);
    task.addAssignee(user2);

    taskRepository.save(task);
  }

  @Test
  void testUpdateTask() {
    Task existing = taskRepository.findById(task.getId()).orElseThrow();
    existing.setTitle("Updated Title");

    taskRepository.save(existing);

    Task updated = taskRepository.findById(task.getId()).orElseThrow();
    assertEquals("Updated Title", updated.getTitle());
  }

  @Test
  void testDeleteTask() {
    taskRepository.delete(task);

    assertFalse(taskRepository.findById(task.getId()).isPresent());
  }

  @Test
  void testFindByGroupId() {
    List<Task> tasks = taskRepository.findByGroupId(group.getId());

    assertEquals(1, tasks.size());
    assertEquals("Test Task", tasks.get(0).getTitle());
  }

  @Test
  void testFindByAssigneesId() {
    List<Task> tasksOfUser1 = taskRepository.findByAssigneesId(user1.getId());

    assertEquals(1, tasksOfUser1.size());
    assertEquals("Test Task", tasksOfUser1.get(0).getTitle());
  }

  @Test
  void testFindAssigneeIdsByTaskId() {
    List<Long> assigneeIds = taskRepository.findAssigneeIdsByTaskId(task.getId());

    assertTrue(assigneeIds.contains(user1.getId()));
    assertTrue(assigneeIds.contains(user2.getId()));
    assertEquals(2, assigneeIds.size());
  }

  @Test
  void testAddAndRemoveTags() {
    task.addTag("backend");
    task.addTag("urgent");
    taskRepository.save(task);

    Task saved = taskRepository.findById(task.getId()).orElseThrow();
    assertNotNull(saved.getTags());
    assertEquals(2, saved.getTags().size());
    assertTrue(saved.getTags().contains("backend"));
    assertTrue(saved.getTags().contains("urgent"));

    saved.removeTag("backend");
    taskRepository.save(saved);
    Task updated = taskRepository.findById(task.getId()).orElseThrow();
    assertEquals(1, updated.getTags().size());
    assertTrue(updated.getTags().contains("urgent"));
  }

  @Test
  void testOverdueTask() {
    task.setDueDate(LocalDateTime.now().minusDays(1));
    taskRepository.save(task);

    Task overdue = taskRepository.findById(task.getId()).orElseThrow();
    assertTrue(overdue.isOverdue());

    overdue.markComplete();
    taskRepository.save(overdue);
    Task completed = taskRepository.findById(task.getId()).orElseThrow();
    assertFalse(completed.isOverdue());
  }

  @Test
  void testAddAndRemoveAssignee() {
    User newUser = new User();
    newUser.setEmail("newuser@example.com");
    newUser.setSurname("New");
    newUser.setLastname("User");
    userRepository.save(newUser);

    task.addAssignee(newUser);
    taskRepository.save(task);

    Task updated = taskRepository.findById(task.getId()).orElseThrow();
    assertTrue(updated.getAssignees().contains(newUser));

    updated.removeAssignee(newUser);
    taskRepository.save(updated);
    Task finalTask = taskRepository.findById(task.getId()).orElseThrow();
    assertFalse(finalTask.getAssignees().contains(newUser));
  }

  @Test
  void testCategoryAndPriority() {
    task.setCategory("Homework");
    task.setPriority(Task.Priority.HIGH);
    taskRepository.save(task);

    Task saved = taskRepository.findById(task.getId()).orElseThrow();
    assertEquals("Homework", saved.getCategory());
    assertEquals(Task.Priority.HIGH, saved.getPriority());
  }
}
