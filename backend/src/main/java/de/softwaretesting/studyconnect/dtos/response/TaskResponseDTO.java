package de.softwaretesting.studyconnect.dtos.response;

import de.softwaretesting.studyconnect.models.Task.Priority;
import de.softwaretesting.studyconnect.models.Task.Status;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.Value;

/** Data Transfer Object for task responses. */
@Value
public class TaskResponseDTO implements Serializable {
  private Long id;
  private String title;
  private String description;
  private LocalDateTime dueDate;
  private Priority priority;
  private Status status;
  private String category;
  private Set<String> tags;
  private Long createdById;
  private Set<Long> assigneeIds;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long groupId;
}
