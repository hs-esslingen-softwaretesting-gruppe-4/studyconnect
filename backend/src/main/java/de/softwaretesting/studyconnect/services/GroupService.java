package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.request.CreateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UpdateGroupRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.GroupResponseDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.CreateGroupRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.GroupResponseMapper;
import de.softwaretesting.studyconnect.models.Group;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

  private static final int INVITE_CODE_SAVE_RETRY_ATTEMPTS = 2;
  private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);
  private static final String GROUP_NOT_FOUND_MESSAGE = "Group not found with id: ";

  private final GroupRepository groupRepository;
  private final UserService userService;
  private final TaskService taskService;
  private final CreateGroupRequestMapper groupRequestMapper;
  private final GroupResponseMapper groupResponseMapper;

  /**
   * Retrieves all public groups.
   *
   * @return a ResponseEntity containing a list of GroupResponseDTOs for all public groups
   */
  public ResponseEntity<List<GroupResponseDTO>> getAllPublicGroups() {
    Optional<List<Group>> publicGroups = groupRepository.findByIsPublicTrue();
    List<GroupResponseDTO> dtoList = groupResponseMapper.toDtoList(publicGroups.orElse(List.of()));
    return ResponseEntity.ok(dtoList);
  }

  /**
   * Retrieves a group by its ID.
   *
   * @param groupId the ID of the group to retrieve
   * @return a ResponseEntity containing the GroupResponseDTO for the specified group
   * @throws NotFoundException if the group with the specified ID does not exist
   */
  public ResponseEntity<GroupResponseDTO> getGroupById(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));
    GroupResponseDTO dto = groupResponseMapper.toDto(group);
    return ResponseEntity.ok(dto);
  }

  /**
   * Retrieves all groups that a user is a member of.
   *
   * @param userId the ID of the user
   * @return a ResponseEntity containing a list of GroupResponseDTOs for groups the user is a member
   *     of
   */
  public ResponseEntity<List<GroupResponseDTO>> getGroupsByUserId(Long userId) {
    Optional<List<Group>> groups = groupRepository.findByMembersId(userId);
    List<GroupResponseDTO> dtoList = groupResponseMapper.toDtoList(groups.orElse(List.of()));
    return ResponseEntity.ok(dtoList);
  }

  /**
   * Creates a new group with the specified details.
   *
   * @param dto the data transfer object containing group details
   * @return a ResponseEntity containing the created group's response DTO
   * @throws BadRequestException if any referenced users do not exist
   * @throws InternalServerErrorException if there is an error saving the group
   */
  public ResponseEntity<GroupResponseDTO> createGroup(CreateGroupRequestDTO dto) {

    Group group = groupRequestMapper.toEntity(dto);

    Set<Long> allReferencedUserIds = new HashSet<>();
    allReferencedUserIds.addAll(dto.getAdminIds());
    allReferencedUserIds.addAll(dto.getMemberIds());
    allReferencedUserIds.add(dto.getCreatedById());
    Map<Long, User> usersById = userService.getUsersByIdMap(allReferencedUserIds);

    Set<Long> missing = new HashSet<>(allReferencedUserIds);
    missing.removeAll(usersById.keySet());
    if (!missing.isEmpty()) {
      throw new BadRequestException("One or more users not found: " + missing);
    }

    for (Long adminId : dto.getAdminIds()) {
      group.getAdmins().add(usersById.get(adminId));
    }
    for (Long memberId : dto.getMemberIds()) {
      group.getMembers().add(usersById.get(memberId));
    }

    group.setCreatedBy(usersById.get(dto.getCreatedById()));
    try {
      Group savedGroup = saveWithInviteCodeRetry(group);
      return new ResponseEntity<>(groupResponseMapper.toDto(savedGroup), HttpStatus.CREATED);
    } catch (DataIntegrityViolationException e) {
      throw new InternalServerErrorException("Error saving group due to database constraint");
    } catch (IllegalStateException e) {
      throw new InternalServerErrorException("Error saving group after multiple attempts");
    }
  }

  /**
   * Updates an existing group with the specified details.
   *
   * @param groupId the ID of the group to update
   * @param dto the data transfer object containing updated group details
   * @return a ResponseEntity containing the updated group's response DTO
   * @throws NotFoundException if the group with the specified ID does not exist
   * @throws BadRequestException if validation fails for member or admin updates
   */
  @Transactional
  public ResponseEntity<GroupResponseDTO> updateGroup(Long groupId, UpdateGroupRequestDTO dto) {

    Group patchGroup =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));

    validateMaxMembersUpdate(patchGroup, dto);
    validateMemberUpdate(patchGroup, dto);
    validateAdminUpdate(patchGroup, dto);
    applyPatch(patchGroup, dto);

    Group updatedGroup = groupRepository.save(patchGroup);
    GroupResponseDTO responseDTO = groupResponseMapper.toDto(updatedGroup);
    return ResponseEntity.ok(responseDTO);
  }

  /**
   * Validates that the new maximum members in the update request is not less than the current
   * member count.
   *
   * @param group the group being updated
   * @param dto the update request DTO
   * @throws BadRequestException if the new maximum members is less than the current member count
   */
  private void validateMaxMembersUpdate(Group group, UpdateGroupRequestDTO dto) {
    if (dto.getMaxMembers() != null) {
      int newMaxMembers = dto.getMaxMembers();
      if (newMaxMembers < group.getMemberCount()) {
        throw new BadRequestException(
            "New maximum members "
                + newMaxMembers
                + " is less than current member count "
                + group.getMemberCount());
      }
    }
  }

  /**
   * Validates that new members in the update request do not exceed the group's maximum member
   * limit.
   *
   * @param group the group being updated
   * @param dto the update request DTO
   * @throws BadRequestException if adding new members would exceed the limit
   */
  private void validateMemberUpdate(Group group, UpdateGroupRequestDTO dto) {
    if (dto.getMemberIds() == null) {
      return;
    }

    Set<Long> existingMemberIds =
        group.getMembers().stream().map(User::getId).collect(Collectors.toSet());
    Set<Long> newMemberIds =
        dto.getMemberIds().stream()
            .filter(memberId -> !existingMemberIds.contains(memberId))
            .collect(Collectors.toSet());

    if (group.getMemberCount() + newMemberIds.size() > group.getMaxMembers()) {
      throw new BadRequestException(
          "Adding these member(s) would exceed the group's maximum member limit");
    }
  }

  /**
   * Validates that all new admins in the update request are also members (either existing or new).
   *
   * @param group the group being updated
   * @param dto the update request DTO
   * @throws BadRequestException if any new admin is not a member or is already an
   */
  private void validateAdminUpdate(Group group, UpdateGroupRequestDTO dto) {
    if (dto.getAdminIds() == null) {
      return;
    }

    // Concatenate existing members and new members from the DTO
    Set<Long> prospectiveMemberIds =
        group.getMembers().stream().map(User::getId).collect(Collectors.toSet());
    if (dto.getMemberIds() != null) {
      prospectiveMemberIds.addAll(dto.getMemberIds());
    }

    Set<Long> existingAdminIds =
        group.getAdmins().stream().map(User::getId).collect(Collectors.toSet());
    Set<Long> newAdminIds =
        dto.getAdminIds().stream()
            .filter(adminId -> !existingAdminIds.contains(adminId))
            .collect(Collectors.toSet());

    for (Long adminId : newAdminIds) {
      if (!prospectiveMemberIds.contains(adminId)) {
        throw new BadRequestException(
            "User with id " + adminId + " must be a member to be an admin of the group");
      }
    }
  }

  /**
   * Applies the updates from the DTO to the group entity.
   *
   * @param group the group entity to update
   * @param dto the update request DTO containing the new values
   */
  private void applyPatch(Group group, UpdateGroupRequestDTO dto) {
    if (dto.getName() != null) {
      group.setName(dto.getName());
    }
    if (dto.getDescription() != null) {
      group.setDescription(dto.getDescription());
    }
    if (dto.getIsPublic() != null) {
      group.setPublic(dto.getIsPublic());
    }
    if (dto.getMemberIds() != null) {
      Set<Long> existingMemberIds =
          group.getMembers().stream().map(User::getId).collect(Collectors.toSet());
      for (Long memberId : dto.getMemberIds()) {
        if (existingMemberIds.contains(memberId)) {
          continue;
        }
        User member = userService.retrieveUserById(memberId);
        group.addMember(member);
      }
    }
    if (dto.getAdminIds() != null) {
      Set<Long> existingAdminIds =
          group.getAdmins().stream().map(User::getId).collect(Collectors.toSet());
      for (Long adminId : dto.getAdminIds()) {
        if (existingAdminIds.contains(adminId)) {
          continue;
        }
        User admin = userService.retrieveUserById(adminId);
        group.addAdmin(admin);
      }
    }
  }

  /**
   * Removes a member from a group. If the member is also an admin, that role is removed as well. If
   * the group has no members left after removal, the group is deleted. If there are still members
   * but no admins, the first member is promoted to admin.
   *
   * @param groupId the ID of the group
   * @param userId the ID of the user to remove as member
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group or user does not exist
   * @throws BadRequestException if the user is not a member of the group
   */
  @Transactional
  public ResponseEntity<Void> removeMemberFromGroup(Long groupId, Long userId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));
    User user = userService.retrieveUserById(userId);
    // First, check if the member is also an admin and remove that role if so
    group.removeAdmin(user);
    boolean removed = group.removeMember(user);
    if (!removed) {
      throw new BadRequestException("User with id " + userId + " is not a member of the group");
    }

    // Remove user from tasks he is assigned to as well
    this.taskService.unassignUserFromAllTasksInGroup(userId, groupId);

    // Check if the group still has any members after removal
    if (group.getMemberCount() == 0) {
      groupRepository.delete(group);
    } else {
      // If no admins remain but there are still members, promote the first member to
      // admin
      if (group.getAdmins().isEmpty() && group.getMemberCount() > 0) {
        try {
          User newAdmin = group.getMembers().iterator().next();
          group.addAdmin(newAdmin);
        } catch (Exception e) {
          throw new InternalServerErrorException(
              "Error promoting member to admin after admin removal: " + e.getMessage());
        }
      }
      groupRepository.save(group);
    }
    return ResponseEntity.noContent().build();
  }

  /**
   * Removes admin role from a user in a group.
   *
   * @param groupId the ID of the group
   * @param userId the ID of the user to remove as admin
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group or user does not exist
   * @throws BadRequestException if the user is not an admin of the group
   */
  @Transactional
  public ResponseEntity<Void> removeAdminFromGroup(Long groupId, Long userId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));
    User user = userService.retrieveUserById(userId);
    boolean removed = group.removeAdmin(user);
    if (!removed) {
      throw new BadRequestException("User with id " + userId + " is not an admin of the group");
    }
    groupRepository.save(group);
    return ResponseEntity.noContent().build();
  }

  /**
   * Deletes a group by its ID.
   *
   * @param groupId the ID of the group to delete
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group with the specified ID does not exist
   */
  public ResponseEntity<Void> deleteGroup(Long groupId) {
    deleteGroupById(groupId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Allows a user to join a group using an invitation code.
   *
   * @param inviteCode the invitation code of the group
   * @param userId the ID of the user attempting to join
   * @return a ResponseEntity with no content
   * @throws NotFoundException if the group with the specified invite code does not exist
   * @throws BadRequestException if the group is full or the user is already a member
   */
  @Transactional
  public ResponseEntity<Void> joinGroupByInvitationCode(String inviteCode, Long userId) {
    Group group =
        groupRepository
            .findByInviteCode(inviteCode)
            .orElseThrow(
                () -> new NotFoundException("Group not found with invite code: " + inviteCode));

    if (group.getMemberCount() >= group.getMaxMembers()) {
      throw new BadRequestException("Group has reached its maximum member limit");
    }

    User user = userService.retrieveUserById(userId);
    boolean added = group.addMember(user);
    if (!added) {
      throw new BadRequestException("User with id " + userId + " is already a member of the group");
    }

    groupRepository.save(group);
    return ResponseEntity.noContent().build();
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Set<UserResponseDTO>> getMembersByGroupId(Long groupId) {
    try {
      Set<Long> memberIds =
          groupRepository
              .findMemberIdsByGroupId(groupId)
              .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));

      Set<UserResponseDTO> memberDTOs = userService.getUsersByIdsDTOs(memberIds);
      return ResponseEntity.ok(memberDTOs);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException("Error retrieving group members: " + e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Set<UserResponseDTO>> getAdminsByGroupId(Long groupId) {
    try {
      Set<Long> adminIds =
          groupRepository
              .findAdminIdsByGroupId(groupId)
              .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));

      Set<UserResponseDTO> adminDTOs = userService.getUsersByIdsDTOs(adminIds);
      return ResponseEntity.ok(adminDTOs);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new InternalServerErrorException("Error retrieving group admins: " + e.getMessage());
    }
  }

  /**
   * Attempts to save the group, retrying with a new invite code if a unique constraint violation
   * occurs on the invite_code column.
   *
   * @param group the group to save
   * @return the saved group
   * @throws DataIntegrityViolationException if a non-invite-code-related database error occurs
   * @throws IllegalStateException if unable to save after the configured number of attempts
   */
  private Group saveWithInviteCodeRetry(Group group) {
    if (group == null) {
      throw new IllegalArgumentException("group must not be null");
    }

    for (int attempt = 1; attempt <= INVITE_CODE_SAVE_RETRY_ATTEMPTS; attempt++) {
      try {
        return groupRepository.saveAndFlush(group);
      } catch (DataIntegrityViolationException ex) {
        boolean inviteCodeViolation = looksLikeInviteCodeUniqueViolation(ex);
        boolean isLastAttempt = attempt == INVITE_CODE_SAVE_RETRY_ATTEMPTS;
        if (!inviteCodeViolation) {
          throw ex;
        }
        if (isLastAttempt) {
          break;
        }

        LOGGER.warn(
            "Attempt {} to save group failed due to invite code conflict. Retrying...", attempt);
        group.regenerateInviteCode();
      }
    }

    // Defensive: loop always returns/throws.
    LOGGER.error(
        "Failed to save group after {} attempts due to invite code conflicts",
        INVITE_CODE_SAVE_RETRY_ATTEMPTS);
    throw new IllegalStateException("Failed to save group after retries");
  }

  /**
   * Analyzes a DataIntegrityViolationException to determine if it was caused by a unique constraint
   * violation on the invite_code column.
   *
   * @param ex the DataIntegrityViolationException to analyze
   * @return true if the exception appears to be due to an invite_code unique constraint violation,
   *     false otherwise
   */
  private static boolean looksLikeInviteCodeUniqueViolation(DataIntegrityViolationException ex) {
    Throwable current = ex;
    while (current != null) {
      if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
        String constraintName = cve.getConstraintName();
        if (constraintName != null && constraintName.toLowerCase().contains("invite")) {
          return true;
        }
      }

      String message = current.getMessage();
      if (message != null && message.toLowerCase().contains("invite_code")) {
        return true;
      }

      current = current.getCause();
    }
    return false;
  }

  private boolean deleteGroupById(Long groupId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));
    try {
      groupRepository.delete(group);
      return true;
    } catch (Exception e) {
      LOGGER.error("Error deleting group with id {}: {}", groupId, e.getMessage());
      throw new InternalServerErrorException("Error deleting group with id: " + groupId);
    }
  }

  public ResponseEntity<List<GroupResponseDTO>> searchPublicGroups(String query) {
    Optional<List<Group>> publicGroups = groupRepository.searchPublicGroupsByName(query);
    List<GroupResponseDTO> dtoList = groupResponseMapper.toDtoList(publicGroups.orElse(List.of()));
    return ResponseEntity.ok(dtoList);
  }

  public ResponseEntity<Void> joinGroupById(Long groupId, Long userId) {
    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new NotFoundException(GROUP_NOT_FOUND_MESSAGE + groupId));
    if (group.getMemberCount() >= group.getMaxMembers()) {
      throw new BadRequestException("Group has reached its maximum member limit");
    }
    User user = userService.retrieveUserById(userId);
    if (group.getMembers().contains(user)) {
      throw new BadRequestException("User with id " + userId + " is already a member of the group");
    }
    group.addMember(user);
    groupRepository.save(group);
    return ResponseEntity.noContent().build();
  }
}
