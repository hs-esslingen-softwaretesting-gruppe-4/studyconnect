package de.softwaretesting.studyconnect.dtos.request;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import de.softwaretesting.studyconnect.models.Task.Priority;
import de.softwaretesting.studyconnect.models.Task.Status;
import lombok.Value;

@Value
public class TaskRequestDTO implements Serializable {
    String title;
    String description;
    LocalDateTime dueDate;
    Priority priority;
    Status status;
    String category;
    Set<String> tags;
    Long createdById;
    Set<Long> assigneeIds;
}
