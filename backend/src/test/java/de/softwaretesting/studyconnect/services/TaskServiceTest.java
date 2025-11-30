package de.softwaretesting.studyconnect.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
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

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private TaskRequestMapper taskRequestMapper;

    @Mock
    private TaskResponseMapper taskResponseMapper;

    @InjectMocks
    private TaskService taskService;

    private TaskRequestDTO taskRequestDTO;
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
        creator.setSurname("Creator");
        creator.setLastname("User");

        assignee1 = new User();
        assignee1.setId(2L);
        assignee1.setEmail("assignee1@example.com");
        assignee1.setSurname("Assignee");
        assignee1.setLastname("One");

        assignee2 = new User();
        assignee2.setId(3L);
        assignee2.setEmail("assignee2@example.com");
        assignee2.setSurname("Assignee");
        assignee2.setLastname("Two");

        // Set up group
        group = new Group();
        group.setId(100L);
        group.setName("Test Group");

        // Set up task request DTO
        Set<Long> assigneeIds = new HashSet<>(Set.of(2L, 3L));
        Set<String> tags = new HashSet<>(Set.of("urgent", "homework"));
        taskRequestDTO = new TaskRequestDTO(
                "Test Task",
                "Test Description",
                LocalDateTime.now().plusDays(7),
                Priority.HIGH,
                Status.OPEN,
                "Category",
                tags,
                1L,
                assigneeIds
        );

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
        savedTask.setAssignees(Set.of(assignee1, assignee2));
        savedTask.setCreatedBy(creator);
        savedTask.setGroup(group);

        // Set up response DTO
        taskResponseDTO = new TaskResponseDTO(
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
                100L
        );
    }

    @Test
    void createTask_WithValidData_ShouldReturnCreatedTask() {
        // Arrange
        Long groupId = 100L;
        when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findAllById(taskRequestDTO.getAssigneeIds())).thenReturn(List.of(assignee1, assignee2));
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

    @Test
    void createTask_WithNonExistentGroup_ShouldThrowNotFoundException() {
        // Arrange
        Long groupId = 999L;
        when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
        when(groupRepository.findById(groupId)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            taskService.createTask(groupId, taskRequestDTO);
        });

        assertEquals("Group not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_WithNonExistentAssignee_ShouldThrowNotFoundException() {
        // Arrange
        Long groupId = 100L;
        when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        // Return only one user when two are expected
        when(userRepository.findAllById(taskRequestDTO.getAssigneeIds())).thenReturn(List.of(assignee1));

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            taskService.createTask(groupId, taskRequestDTO);
        });

        assertEquals("One or more assignee users not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createTask_WithNonExistentCreator_ShouldThrowNotFoundException() {
        // Arrange
        Long groupId = 100L;
        when(taskRequestMapper.toEntity(taskRequestDTO)).thenReturn(task);
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(userRepository.findAllById(taskRequestDTO.getAssigneeIds())).thenReturn(List.of(assignee1, assignee2));
        when(userRepository.findById(taskRequestDTO.getCreatedById())).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            taskService.createTask(groupId, taskRequestDTO);
        });

        assertEquals("Creator user not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }
}
