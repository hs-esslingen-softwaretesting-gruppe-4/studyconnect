package de.softwaretesting.studyconnect.dtos.request;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import de.softwaretesting.studyconnect.models.Task.Priority;
import de.softwaretesting.studyconnect.models.Task.Status;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

@Value
public class TaskRequestDTO implements Serializable {
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description;

    @Future(message = "Due date must be in the future")
    LocalDateTime dueDate;

    @NotNull(message = "Priority is required")
    Priority priority;

    Status status;

    String category;

    Set<String> tags;

    @NotNull(message = "Creator ID is required")
    Long createdById;

    @NotEmpty(message = "At least one assignee ID is required")
    Set<Long> assigneeIds;
}
