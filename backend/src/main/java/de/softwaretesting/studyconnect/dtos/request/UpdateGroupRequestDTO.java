package de.softwaretesting.studyconnect.dtos.request;

import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Value;

/** Data Transfer Object for updating an existing group. */
@Value
public class UpdateGroupRequestDTO {

  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  private Boolean isPublic;

  private Set<Long> memberIds;

  private Set<Long> adminIds;

  private Integer maxMembers;
}
