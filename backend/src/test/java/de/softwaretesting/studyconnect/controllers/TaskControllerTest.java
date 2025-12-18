package de.softwaretesting.studyconnect.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.TaskResponseDTO;
import de.softwaretesting.studyconnect.models.Task;
import de.softwaretesting.studyconnect.services.TaskService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@WithMockUser
@ActiveProfiles("test")
@Import(TaskControllerTest.TestSecurityConfig.class)
@DisplayName("TaskController Tests")
class TaskControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private TaskService taskService;

  @Autowired private ObjectMapper objectMapper;

  @TestConfiguration
  static class TestSecurityConfig {
    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
      return http.csrf(csrf -> csrf.disable())
          .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .build();
    }
  }

  @Test
  @DisplayName("Should get all tasks by group ID")
  void shouldGetAllTasksByGroupId() throws Exception {
    // Given
    Long groupId = 1L;
    List<TaskResponseDTO> tasks = createTaskResponseList();
    given(taskService.getAllTasksInGroup(groupId)).willReturn(ResponseEntity.ok(tasks));

    // When & Then
    mockMvc
        .perform(get("/api/tasks/groups/{groupId}", groupId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(1))
        .andExpect(jsonPath("$[0].title").value("Test Task 1"))
        .andExpect(jsonPath("$[1].id").value(2))
        .andExpect(jsonPath("$[1].title").value("Test Task 2"));

    verify(taskService).getAllTasksInGroup(groupId);
  }

  @Test
  @DisplayName("Should get tasks by user ID")
  void shouldGetTasksByUserId() throws Exception {
    // Given
    Long userId = 1L;
    List<TaskResponseDTO> tasks = createTaskResponseList();
    given(taskService.getAllTasksAssignedToUser(userId)).willReturn(ResponseEntity.ok(tasks));

    // When & Then
    mockMvc
        .perform(get("/api/tasks/users/{userId}", userId))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(2));

    verify(taskService).getAllTasksAssignedToUser(userId);
  }

  @Test
  @DisplayName("Should create task successfully")
  void shouldCreateTaskSuccessfully() throws Exception {
    // Given
    Long groupId = 1L;
    TaskRequestDTO requestDTO = createValidTaskRequestDTO();
    TaskResponseDTO responseDTO = createTaskResponseDTO();
    given(taskService.createTask(eq(groupId), any(TaskRequestDTO.class)))
        .willReturn(ResponseEntity.ok(responseDTO));

    // When & Then
    mockMvc
        .perform(
            post("/api/tasks/groups/{groupId}", groupId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Test Task 1"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.status").value("OPEN"));

    verify(taskService).createTask(eq(groupId), any(TaskRequestDTO.class));
  }

  @Test
  @DisplayName("Should fail to create task with invalid data")
  void shouldFailToCreateTaskWithInvalidData() throws Exception {
    // Given
    Long groupId = 1L;
    TaskRequestDTO invalidRequestDTO = createInvalidTaskRequestDTO();

    // When & Then
    mockMvc
        .perform(
            post("/api/tasks/groups/{groupId}", groupId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should update task successfully")
  void shouldUpdateTaskSuccessfully() throws Exception {
    // Given
    Long taskId = 1L;
    TaskRequestDTO requestDTO = createValidTaskRequestDTO();
    TaskResponseDTO responseDTO = createTaskResponseDTO();
    given(taskService.updateTask(eq(taskId), any(TaskRequestDTO.class)))
        .willReturn(ResponseEntity.ok(responseDTO));

    // When & Then
    mockMvc
        .perform(
            put("/api/tasks/{taskId}", taskId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.title").value("Test Task 1"));

    verify(taskService).updateTask(eq(taskId), any(TaskRequestDTO.class));
  }

  @Test
  @DisplayName("Should fail to update task with invalid data")
  void shouldFailToUpdateTaskWithInvalidData() throws Exception {
    // Given
    Long taskId = 1L;
    TaskRequestDTO invalidRequestDTO = createInvalidTaskRequestDTO();

    // When & Then
    mockMvc
        .perform(
            put("/api/tasks/{taskId}", taskId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDTO)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Should delete task successfully")
  void shouldDeleteTaskSuccessfully() throws Exception {
    // Given
    Long taskId = 1L;
    given(taskService.deleteTask(taskId)).willReturn(ResponseEntity.noContent().build());

    // When & Then
    mockMvc.perform(delete("/api/tasks/{taskId}", taskId)).andExpect(status().isNoContent());

    verify(taskService).deleteTask(taskId);
  }

  @Test
  @DisplayName("Should handle invalid path variables")
  void shouldHandleInvalidPathVariables() throws Exception {
    // When & Then
    mockMvc.perform(get("/api/tasks/groups/invalid")).andExpect(status().isBadRequest());

    mockMvc.perform(get("/api/tasks/users/invalid")).andExpect(status().isBadRequest());
  }

  // Helper methods

  private TaskRequestDTO createValidTaskRequestDTO() {
    return new TaskRequestDTO(
        "Test Task 1",
        "Test Description",
        LocalDateTime.now().plusDays(1),
        Task.Priority.HIGH,
        Task.Status.OPEN,
        "Test Category",
        Set.of("tag1", "tag2"),
        1L,
        Set.of(1L, 2L));
  }

  private TaskRequestDTO createInvalidTaskRequestDTO() {
    return new TaskRequestDTO(
        "", // Invalid: blank title
        "Test Description",
        LocalDateTime.now().minusDays(1), // Invalid: past due date
        null, // Invalid: null priority
        Task.Status.OPEN,
        "Test Category",
        Set.of("tag1"),
        null, // Invalid: null creator ID
        Set.of()); // Invalid: empty assignee set
  }

  private TaskResponseDTO createTaskResponseDTO() {
    return new TaskResponseDTO(
        1L,
        "Test Task 1",
        "Test Description",
        LocalDateTime.now().plusDays(1),
        Task.Priority.HIGH,
        Task.Status.OPEN,
        "Test Category",
        Set.of("tag1", "tag2"),
        1L,
        Set.of(1L, 2L),
        LocalDateTime.now(),
        LocalDateTime.now(),
        1L);
  }

  private List<TaskResponseDTO> createTaskResponseList() {
    TaskResponseDTO task1 =
        new TaskResponseDTO(
            1L,
            "Test Task 1",
            "Test Description 1",
            LocalDateTime.now().plusDays(1),
            Task.Priority.HIGH,
            Task.Status.OPEN,
            "Category 1",
            Set.of("tag1"),
            1L,
            Set.of(1L),
            LocalDateTime.now(),
            LocalDateTime.now(),
            1L);

    TaskResponseDTO task2 =
        new TaskResponseDTO(
            2L,
            "Test Task 2",
            "Test Description 2",
            LocalDateTime.now().plusDays(2),
            Task.Priority.MEDIUM,
            Task.Status.IN_PROGRESS,
            "Category 2",
            Set.of("tag2"),
            2L,
            Set.of(2L),
            LocalDateTime.now(),
            LocalDateTime.now(),
            1L);

    return Arrays.asList(task1, task2);
  }
}
