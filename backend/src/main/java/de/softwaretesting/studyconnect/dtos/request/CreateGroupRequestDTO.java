package de.softwaretesting.studyconnect.dtos.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Value;

/** Data Transfer Object for creating a new group. */
@Value
public class CreateGroupRequestDTO {

  @NotBlank(message = "Name is required")
  @Size(max = 100, message = "Name must not exceed 100 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  @JsonProperty("is_public")
  @NotNull(message = "Privacy setting is_public is required")
  private Boolean isPublic;

  @NotNull(message = "max_members is required")
  private Integer maxMembers;

  @NotEmpty(message = "At least one member ID is required")
  private Set<Long> memberIds;

  @NotEmpty(message = "At least one admin ID is required")
  private Set<Long> adminIds;

  @NotNull(message = "created_by_id is required")
  private Long createdById;

  @AssertTrue(message = "Creator must be included in member_ids")
  private boolean isCreatorInMembers() {
    return memberIds != null && createdById != null && memberIds.contains(createdById);
  }

  @AssertTrue(message = "Admin IDs must be a subset of member_ids")
  private boolean areAdminsSubsetOfMembers() {
    return memberIds != null && adminIds != null && memberIds.containsAll(adminIds);
  }
}
