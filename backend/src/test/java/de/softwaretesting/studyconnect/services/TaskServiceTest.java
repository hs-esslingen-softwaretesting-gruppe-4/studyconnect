package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UpdateTaskRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.TaskResponseDTO;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.TaskRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.TaskResponseMapper;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.Task.Priority;
import de.softwaretesting.studyconnect.models.Task.Status;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;

  @Mock private UserRepository userRepository;

  @Mock private GroupRepository groupRepository;

  @Mock private TaskRequestMapper taskRequestMapper;

  @Mock private TaskResponseMapper taskResponseMapper;

  @InjectMocks private TaskService taskService;

  private TaskRequestDTO taskRequestDTO;
  private UpdateTaskRequestDTO updateTaskRequestDTO;
  private Task task;
  private Task savedTask;
  private TaskResponseDTO taskResponseDTO;
  private Group group;
  private User creator;
  private User assignee1;
  private User assignee2;

  @BeforeEach
  void setUp() {
    // Set up users
    creator = new User();
    creator.setId(1L);
    creator.setEmail("creator@example.com");
    creator.setFirstname("Creator");
    creator.setLastname("User");

    assignee1 = new User();
    assignee1.setId(2L);
    assignee1.setEmail("assignee1@example.com");
    assignee1.setFirstname("Assignee");
    assignee1.setLastname("One");

    assignee2 = new User();
    assignee2.setId(3L);
    assignee2.setEmail("assignee2@example.com");
    assignee2.setFirstname("Assignee");
    assignee2.setLastname("Two");

    // Set up group
    group = new Group();
    group.setId(100L);
    group.setName("Test Group");

    // Set up task request DTO
    Set<Long> assigneeIds = new HashSet<>(Set.of(2L, 3L));
    Set<String> tags = new HashSet<>(Set.of("urgent", "homework"));
    taskRequestDTO =
        new TaskRequestDTO(
            "Test Task",
            "Test Description",
            LocalDateTime.now().plusDays(7),
            Priority.HIGH,
            Status.OPEN,
            "Category",
            tags,
            1L,
            assigneeIds);

    // Set up update task request DTO
    updateTaskRequestDTO =
        new UpdateTaskRequestDTO(
            "Test Task",
            "Test Description",
            LocalDateTime.now().plusDays(7),
            Priority.HIGH,
            Status.OPEN,
            "Category",
            tags,
            1L,
            assigneeIds);

    // Set up task entity
    task = new Task();
    task.setTitle("Test Task");
    task.setDescription("Test Description");
    task.setDueDate(LocalDateTime.now().plusDays(7));
    task.setPriority(Priority.HIGH);
    task.setStatus(Status.OPEN);
    task.setCategory("Category");
    task.setTags(new HashSet<>());
    task.setAssignees(new HashSet<>());

    // Set up saved task (with ID)
    savedTask = new Task();
    savedTask.setId(1L);
    savedTask.setTitle("Test Task");
    savedTask.setDescription("Test Description");
    savedTask.setDueDate(task.getDueDate());
    savedTask.setPriority(Priority.HIGH);
    savedTask.setStatus(Status.OPEN);
    savedTask.setCategory("Category");
    savedTask.setTags(tags);
    savedTask.setAssignees(new HashSet<>(Set.of(assignee1, assignee2)));
    savedTask.setCreatedBy(creator);
    savedTask.setGroup(group);

    // Set up response DTO
    taskResponseDTO =
        new TaskResponseDTO(
            1L,
            "Test Task",
            "Test Description",
            savedTask.getDueDate(),
            Priority.HIGH,
            Status.OPEN,
            "Category",
            tags,
            1L,
            assigneeIds,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            100L);
  }

  // ==================== createTask Tests ====================

  /** Creates a new task with valid data and verifies the response. */
  @Test
  void createTask_WithValidData_ShouldReturnCreatedTask() {
    // Arrange
    Long groupId = 100L;
    when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(userRepository.findAllById(taskRequestDTO.getAssigneeIds()))
        .thenReturn(List.of(assignee1, assignee2));
    when(userRepository.findById(taskRequestDTO.getCreatedById())).thenReturn(Optional.of(creator));
    when(taskRepository.save(any(Task.class))).thenReturn(savedTask);
    when(taskResponseMapper.toDto(savedTask)).thenReturn(taskResponseDTO);

    // Act
    ResponseEntity<TaskResponseDTO> response = taskService.createTask(groupId, taskRequestDTO);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(taskResponseDTO.getId(), response.getBody().getId());
    assertEquals(taskResponseDTO.getTitle(), response.getBody().getTitle());
    assertEquals(taskResponseDTO.getPriority(), response.getBody().getPriority());
    assertEquals(taskResponseDTO.getAssigneeIds(), response.getBody().getAssigneeIds());
    assertEquals(taskResponseDTO.getCreatedById(), response.getBody().getCreatedById());
    assertEquals(taskResponseDTO.getTags(), response.getBody().getTags());

    // Verify interactions
    verify(taskRequestMapper).toEntity(taskRequestDTO);
    verify(groupRepository).findById(groupId);
    verify(userRepository).findAllById(taskRequestDTO.getAssigneeIds());
    verify(userRepository).findById(taskRequestDTO.getCreatedById());
    verify(taskRepository).save(any(Task.class));
    verify(taskResponseMapper).toDto(savedTask);
  }

  /**
   * Creates a new task with a non-existent group and verifies that a NotFoundException is thrown.
   */
  @Test
  void createTask_WithNonExistentGroup_ShouldThrowNotFoundException() {
    // Arrange
    Long groupId = 999L;
    when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
    when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

    // Act & Assert
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> {
              taskService.createTask(groupId, taskRequestDTO);
            });

    assertEquals("Group not found", exception.getMessage());
    verify(taskRepository, never()).save(any(Task.class));
  }

  /**
   * Creates a new task with a non-existent assignee and verifies that a NotFoundException is
   * thrown.
   */
  @Test
  void createTask_WithNonExistentAssignee_ShouldThrowNotFoundException() {
    // Arrange
    Long groupId = 100L;
    when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    // Return only one user when two are expected
    when(userRepository.findAllById(taskRequestDTO.getAssigneeIds()))
        .thenReturn(List.of(assignee1));

    // Act & Assert
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> {
              taskService.createTask(groupId, taskRequestDTO);
            });

    assertEquals("One or more assignee users not found", exception.getMessage());
    verify(taskRepository, never()).save(any(Task.class));
  }

  /**
   * Creates a new task with a non-existent creator and verifies that a NotFoundException is thrown.
   */
  @Test
  void createTask_WithNonExistentCreator_ShouldThrowNotFoundException() {
    // Arrange
    Long groupId = 100L;
    when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(userRepository.findAllById(taskRequestDTO.getAssigneeIds()))
        .thenReturn(List.of(assignee1, assignee2));
    when(userRepository.findById(taskRequestDTO.getCreatedById())).thenReturn(Optional.empty());

    // Act & Assert
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> {
              taskService.createTask(groupId, taskRequestDTO);
            });

    assertEquals("Creator user not found", exception.getMessage());
    verify(taskRepository, never()).save(any(Task.class));
  }

  // ==================== getAllTasksInGroup Tests ====================

  /** Retrieves all tasks within an existing group and verifies the response. */
  @Test
  void getAllTasksInGroup_WithExistingGroup_ShouldReturnTasks() {
    // Arrange
    Long groupId = 100L;
    Task task2 = new Task();
    task2.setId(2L);
    task2.setTitle("Second Task");
    task2.setGroup(group);

    TaskResponseDTO taskResponseDTO2 =
        new TaskResponseDTO(
            2L,
            "Second Task",
            "Description",
            LocalDateTime.now().plusDays(5),
            Priority.LOW,
            Status.OPEN,
            "Category",
            Set.of(),
            1L,
            Set.of(2L),
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            100L);

    when(taskRepository.findByGroupId(groupId)).thenReturn(List.of(savedTask, task2));
    when(taskResponseMapper.toDto(savedTask)).thenReturn(taskResponseDTO);
    when(taskResponseMapper.toDto(task2)).thenReturn(taskResponseDTO2);

    // Act
    ResponseEntity<List<TaskResponseDTO>> response = taskService.getAllTasksInGroup(groupId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(2, response.getBody().size());
    assertEquals(taskResponseDTO.getId(), response.getBody().get(0).getId());
    assertEquals(taskResponseDTO2.getId(), response.getBody().get(1).getId());

    verify(taskRepository).findByGroupId(groupId);
  }

  /**
   * Retrieves all tasks within a group that has no tasks and verifies the response is an empty
   * list.
   */
  @Test
  void getAllTasksInGroup_WithNoTasks_ShouldReturnEmptyList() {
    // Arrange
    Long groupId = 100L;
    when(taskRepository.findByGroupId(groupId)).thenReturn(List.of());

    // Act
    ResponseEntity<List<TaskResponseDTO>> response = taskService.getAllTasksInGroup(groupId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().size());

    verify(taskRepository).findByGroupId(groupId);
  }

  // ==================== getAllTasksAssignedToUser Tests ====================

  /** Retrieves all tasks assigned to an existing user and verifies the response. */
  @Test
  void getAllTasksAssignedToUser_WithExistingUser_ShouldReturnTasks() {
    // Arrange
    Long userId = 2L;
    when(taskRepository.findByAssigneesId(userId)).thenReturn(List.of(savedTask));
    when(taskResponseMapper.toDto(savedTask)).thenReturn(taskResponseDTO);

    // Act
    ResponseEntity<List<TaskResponseDTO>> response = taskService.getAllTasksAssignedToUser(userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    assertEquals(taskResponseDTO.getId(), response.getBody().get(0).getId());

    verify(taskRepository).findByAssigneesId(userId);
  }

  /**
   * Retrieves all tasks assigned to a user with no assigned tasks and verifies the response is an
   * empty list.
   */
  @Test
  void getAllTasksAssignedToUser_WithNoAssignedTasks_ShouldReturnEmptyList() {
    // Arrange
    Long userId = 999L;
    when(taskRepository.findByAssigneesId(userId)).thenReturn(List.of());

    // Act
    ResponseEntity<List<TaskResponseDTO>> response = taskService.getAllTasksAssignedToUser(userId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().size());

    verify(taskRepository).findByAssigneesId(userId);
  }

  // ==================== deleteTask Tests ====================

  /** Deletes an existing task and verifies the response. */
  @Test
  void deleteTask_WithExistingTask_ShouldReturnNoContent() {
    // Arrange
    Long taskId = 1L;
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));

    // Act
    ResponseEntity<Void> response = taskService.deleteTask(taskId);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(taskRepository).findById(taskId);
    verify(taskRepository).delete(savedTask);
  }

  /** Attempts to delete a non-existent task and verifies that a NotFoundException is thrown. */
  @Test
  void deleteTask_WithNonExistentTask_ShouldThrowNotFoundException() {
    // Arrange
    Long taskId = 999L;
    when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

    // Act & Assert
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> {
              taskService.deleteTask(taskId);
            });

    assertEquals("Task not found", exception.getMessage());
    verify(taskRepository).findById(taskId);
    verify(taskRepository, never()).delete(any(Task.class));
  }

  // ==================== updateTask Tests ====================

  /** Updates an existing task with valid data and verifies the response. */
  @Test
  void updateTask_WithValidData_ShouldReturnUpdatedTask() {
    // Arrange
    Long taskId = 1L;
    Set<Long> assigneeIds = new HashSet<>(Set.of(2L, 3L));
    Set<String> updatedTags = new HashSet<>(Set.of("updated", "important"));
    UpdateTaskRequestDTO updateRequestDTO =
        new UpdateTaskRequestDTO(
            "Updated Task",
            "Updated Description",
            LocalDateTime.now().plusDays(14),
            Priority.LOW,
            Status.IN_PROGRESS,
            "Updated Category",
            updatedTags,
            1L,
            assigneeIds);

    Task savedUpdatedTask = new Task();
    savedUpdatedTask.setId(1L);
    savedUpdatedTask.setTitle("Updated Task");
    savedUpdatedTask.setDescription("Updated Description");
    savedUpdatedTask.setPriority(Priority.LOW);
    savedUpdatedTask.setStatus(Status.IN_PROGRESS);
    savedUpdatedTask.setCategory("Updated Category");
    savedUpdatedTask.setTags(updatedTags);
    savedUpdatedTask.setAssignees(Set.of(assignee1, assignee2));
    savedUpdatedTask.setCreatedBy(creator);
    savedUpdatedTask.setGroup(group);

    TaskResponseDTO updatedResponseDTO =
        new TaskResponseDTO(
            1L,
            "Updated Task",
            "Updated Description",
            LocalDateTime.now().plusDays(14),
            Priority.LOW,
            Status.IN_PROGRESS,
            "Updated Category",
            updatedTags,
            1L,
            assigneeIds,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            100L);

    when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
    when(userRepository.findAllById(assigneeIds)).thenReturn(List.of(assignee1, assignee2));
    when(taskRepository.save(any(Task.class))).thenReturn(savedUpdatedTask);
    when(taskResponseMapper.toDto(savedUpdatedTask)).thenReturn(updatedResponseDTO);

    // Act
    ResponseEntity<TaskResponseDTO> response = taskService.updateTask(taskId, updateRequestDTO);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals("Updated Task", response.getBody().getTitle());
    assertEquals(Priority.LOW, response.getBody().getPriority());
    assertEquals(Status.IN_PROGRESS, response.getBody().getStatus());

    verify(taskRepository).findById(taskId);
    verify(userRepository).findAllById(assigneeIds);
    verify(taskRepository).save(any(Task.class));
    verify(taskResponseMapper).toDto(savedUpdatedTask);
  }

  /** Attempts to update a non-existent task and verifies that a NotFoundException is thrown. */
  @Test
  void updateTask_WithNonExistentTask_ShouldThrowNotFoundException() {
    // Arrange
    Long taskId = 999L;
    when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

    // Act & Assert
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> {
              taskService.updateTask(taskId, updateTaskRequestDTO);
            });

    assertEquals("Task not found", exception.getMessage());
    verify(taskRepository).findById(taskId);
    verify(taskRepository, never()).save(any(Task.class));
  }

  /**
   * Attempts to update a task with a non-existent assignee and verifies that a NotFoundException is
   * thrown.
   */
  @Test
  void updateTask_WithNonExistentAssignee_ShouldThrowNotFoundException() {
    // Arrange
    Long taskId = 1L;
    when(taskRepository.findById(taskId)).thenReturn(Optional.of(savedTask));
    // Return only one user when two are expected
    when(userRepository.findAllById(taskRequestDTO.getAssigneeIds()))
        .thenReturn(List.of(assignee1));

    // Act & Assert
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> {
              taskService.updateTask(taskId, updateTaskRequestDTO);
            });

    assertEquals("One or more assignee users not found", exception.getMessage());
    verify(taskRepository).findById(taskId);
    verify(taskRepository, never()).save(any(Task.class));
  }
}
