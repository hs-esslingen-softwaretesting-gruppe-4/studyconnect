package de.softwaretesting.studyconnect.controller;

import de.softwaretesting.studyconnect.repositories.*;
import de.softwaretesting.studyconnect.services.TaskService;
import de.softwaretesting.studyconnect.dtos.request.TaskRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.TaskResponseDTO;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class taskController {

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    TaskService TaskService;

    @GetMapping("/tasks/groups/{groupId}")
    public ResponseEntity<List<TaskResponseDTO>> getAllTasksByGroupId(@PathVariable("groupId") Long groupId) {
        try {
            return TaskService.getAllTasksInGroup(groupId);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/tasks/users/{userId}")
    public ResponseEntity<List<TaskResponseDTO>> getTaskById(@PathVariable("id") Long userId) {
        try {
            return TaskService.getAllTasksAssignedToUser(userId);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tasks")
    public ResponseEntity<TaskResponseDTO> createTask(@RequestBody Long groupId, TaskRequestDTO task) {
        try {
            return TaskService.createTask(groupId, task);
        } catch (NotFoundException n) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

	@PutMapping("/tasks/{taskId}")
	public ResponseEntity<TaskResponseDTO> updateTask(@PathVariable("taskId") Long taskId, @RequestBody TaskRequestDTO task) {
		try {
            return TaskService.updateTask(taskId, task);
        } catch (NotFoundException n) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable("taskId") long taskId) {
		try {
            return TaskService.deleteTask(taskId);
		} catch (NotFoundException n) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}