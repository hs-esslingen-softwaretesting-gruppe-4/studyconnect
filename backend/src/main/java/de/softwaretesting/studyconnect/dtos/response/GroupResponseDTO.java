package de.softwaretesting.studyconnect.dtos.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Value;

/** Data Transfer Object for group responses. */
@Value
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class GroupResponseDTO {

  private Long id;
  private String name;
  private String description;

  @JsonProperty("is_public")
  private boolean isPublic;

  private Long createdById;
  private LocalDateTime createdAt;
  private LocalDateTime lastUpdatedAt;
  private int memberCount;
  private int maxMembers;
  private String inviteCode;
}
