package de.softwaretesting.studyconnect.controller;

import de.softwaretesting.studyconnect.repositories.*;
import de.softwaretesting.studyconnect.models.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class taskController {

    private final TaskRepository taskRepository;

    public taskController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    @PostMapping
    public Task createTask(@RequestBody Task task) {
        return taskRepository.save(task);
    }

    @GetMapping("/api/tasks/{id}")
    public Task getTaskById(@RequestBody Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @PutMapping("/api/tasks/{id}")
    public Task updateTask(@RequestBody Task task) {
        return taskRepository.save(task);
    }

    @DeleteMapping("/api/tasks/{id}")
    public void deleteTask(@RequestBody Task task) {
        taskRepository.delete(task);
    }
}