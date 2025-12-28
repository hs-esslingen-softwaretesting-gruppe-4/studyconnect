package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.TaskResponseDTO;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.TaskRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.TaskResponseMapper;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service class for managing tasks within the StudyConnect application. */
@Service
@AllArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final GroupRepository groupRepository;
  private final TaskRequestMapper taskRequestMapper;
  private final TaskResponseMapper taskResponseMapper;

  /**
   * Creates a new task in the specified group.
   *
   * @param groupId the ID of the group to which the task belongs
   * @param taskRequestDTO the data transfer object containing task details
   * @return a ResponseEntity containing the created task's response DTO
   * @throws NotFoundException if the group or any user is not found
   */
  public ResponseEntity<TaskResponseDTO> createTask(Long groupId, TaskRequestDTO taskRequestDTO) {

    // Map DTO to entity
    Task task = taskRequestMapper.toEntity(taskRequestDTO);
    task.setGroup(
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found")));

    // Assign users to the task
    Set<Long> assigneeIds = taskRequestDTO.getAssigneeIds();
    List<User> assignees = userRepository.findAllById(assigneeIds);
    if (assignees.size() != assigneeIds.size()) {
      throw new NotFoundException("One or more assignee users not found");
    }
    task.getAssignees().addAll(assignees);
    task.getTags().addAll(taskRequestDTO.getTags());
    task.setCreatedBy(
        this.userRepository
            .findById(taskRequestDTO.getCreatedById())
            .orElseThrow(() -> new NotFoundException("Creator user not found")));

    // Save and return task
    Task savedTask = taskRepository.save(task);
    TaskResponseDTO taskResponseDTO = taskResponseMapper.toDto(savedTask);
    return new ResponseEntity<>(taskResponseDTO, HttpStatus.CREATED);
  }

  /**
   * Retrieves all tasks within a specific group.
   *
   * @param groupId the ID of the group
   * @return a ResponseEntity containing a list of task response DTOs
   */
  @Transactional(readOnly = true)
  public ResponseEntity<List<TaskResponseDTO>> getAllTasksInGroup(Long groupId) {
    List<Task> tasks = taskRepository.findByGroupId(groupId);
    List<TaskResponseDTO> taskResponseDTOs = tasks.stream().map(taskResponseMapper::toDto).toList();
    return ResponseEntity.ok(taskResponseDTOs);
  }

  /**
   * Retrieves all tasks assigned to a specific user.
   *
   * @param userId the ID of the user
   * @return a ResponseEntity containing a list of task response DTOs
   */
  @Transactional(readOnly = true)
  public ResponseEntity<List<TaskResponseDTO>> getAllTasksAssignedToUser(Long userId) {
    List<Task> tasks = taskRepository.findByAssigneesId(userId);
    List<TaskResponseDTO> taskResponseDTOs = tasks.stream().map(taskResponseMapper::toDto).toList();
    return ResponseEntity.ok(taskResponseDTOs);
  }

  /**
   * Deletes a task by its ID.
   *
   * @param taskId the ID of the task to delete
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the task is not found
   */
  public ResponseEntity<Void> deleteTask(Long taskId) {
    Task task =
        taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
    taskRepository.delete(task);
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates an existing task.
   *
   * @param taskId the ID of the task to update
   * @param taskRequestDTO the data transfer object containing updated task details
   * @return a ResponseEntity containing the updated task's response DTO
   * @throws NotFoundException if the task or any user is not found
   */
  public ResponseEntity<TaskResponseDTO> updateTask(Long taskId, TaskRequestDTO taskRequestDTO) {
    Task existingTask =
        taskRepository.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));

    Task updatedTask = taskRequestMapper.toEntity(taskRequestDTO);
    updatedTask.setId(existingTask.getId());
    updatedTask.setCreatedAt(existingTask.getCreatedAt());
    updatedTask.setGroup(existingTask.getGroup());
    updatedTask.setCreatedBy(existingTask.getCreatedBy());

    // Fetch all assignees in a single query to avoid N+1 problem
    Set<Long> assigneeIds = taskRequestDTO.getAssigneeIds();
    List<User> assignees = userRepository.findAllById(assigneeIds);
    if (assignees.size() != assigneeIds.size()) {
      throw new NotFoundException("One or more assignee users not found");
    }
    updatedTask.getAssignees().clear();
    updatedTask.getAssignees().addAll(assignees);
    updatedTask.getTags().clear();
    updatedTask.getTags().addAll(taskRequestDTO.getTags());

    Task savedTask = taskRepository.save(updatedTask);
    TaskResponseDTO taskResponseDTO = taskResponseMapper.toDto(savedTask);
    return ResponseEntity.ok(taskResponseDTO);
  }
}
