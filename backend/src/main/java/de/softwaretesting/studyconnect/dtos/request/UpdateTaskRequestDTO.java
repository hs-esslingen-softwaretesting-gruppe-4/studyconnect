package de.softwaretesting.studyconnect.dtos.request;

import de.softwaretesting.studyconnect.models.Task.Priority;
import de.softwaretesting.studyconnect.models.Task.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Value;

/** Data Transfer Object for creating or updating a task. */
@Value
public class UpdateTaskRequestDTO implements Serializable {
  @NotBlank(message = "Title is required")
  @Size(max = 200, message = "Title must not exceed 200 characters")
  private String title;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  private LocalDateTime dueDate;

  @NotNull(message = "Priority is required")
  private Priority priority;

  private Status status;

  private String category;

  private Set<String> tags;

  @NotNull(message = "Creator ID is required")
  private Long createdById;

  @NotEmpty(message = "At least one assignee ID is required")
  private Set<Long> assigneeIds;
}
