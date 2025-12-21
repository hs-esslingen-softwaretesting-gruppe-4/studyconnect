package de.softwaretesting.studyconnect.dtos.response;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Value;

/** Data Transfer Object for group responses. */
@Value
public class GroupResponseDTO {

  private Long id;
  private String name;
  private String description;
  private boolean isPrivate;
  private Long createdById;
  private LocalDateTime createdAt;
  private LocalDateTime lastUpdatedAt;
  private int memberCount;
  private int maxMembers;
  private String inviteCode;
  private Set<Long> adminIds;
}
