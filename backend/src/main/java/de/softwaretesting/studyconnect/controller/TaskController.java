package de.softwaretesting.studyconnect.controller;

import de.softwaretesting.studyconnect.services.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.TaskResponseDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TaskController {

    TaskService TaskService;

    @GetMapping("/tasks/groups/{groupId}")
    public ResponseEntity<List<TaskResponseDTO>> getAllTasksByGroupId(@PathVariable("groupId") Long groupId) {
        return TaskService.getAllTasksInGroup(groupId);
    }

    @GetMapping("/tasks/users/{userId}")
    public ResponseEntity<List<TaskResponseDTO>> getTaskByUserId(@PathVariable("userId") Long userId) {
        return TaskService.getAllTasksAssignedToUser(userId);
    }

   @PostMapping("/tasks/groups/{groupId}")
    public ResponseEntity<TaskResponseDTO> createTask(@PathVariable("groupId") Long groupId, @Valid @RequestBody TaskRequestDTO task) {
        return TaskService.createTask(groupId, task);
    }

	@PutMapping("/tasks/{taskId}")
	public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable("taskId") Long taskId, @Valid @RequestBody TaskRequestDTO task) {
		return TaskService.updateTask(taskId, task);
	}

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable("taskId") long taskId) {
		return TaskService.deleteTask(taskId);
    }
}